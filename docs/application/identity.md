# Identity

Accounts and companies. Keycloak owns accounts, credentials, e-mail verification and login (ADR 0006); Talmar keeps
its own `company` and `app_user` and everything that decides access to company data.

This page covers what is built today: the domain model, its persistence and the Keycloak adapter. The backend's own
registration flow (ADR 0003) has been removed. Nothing creates a company or a user yet, and nothing calls the adapter
yet: onboarding a logged-in Keycloak user (`POST /onboarding`), `GET /me`, owner approval and securing the other
endpoints follow in the next PRs of ADR 0006.

## Domain model

Two aggregates in `domain/identity`, linked by id only in the domain (a user holds `companyId`). In the database
`app_user.company_id` is a foreign key to `company`: one company, many users.

`Company`

| Field | Notes |
|-------|-------|
| `id` | UUIDv7. |
| `name` | Derived from the e-mail domain when the company is created: the registrable label, capitalised (`acme.nl` and `sales.acme.co.uk` → "Acme"; `co`, `com`, `org`, `net`, `ac`, `gov`, `edu` before the top-level domain are skipped). There is no endpoint to correct it yet. |
| `emailDomain` | Lower case, unique. The key for "same company". |
| `lastModifiedBy` | Required. The `app_user` id of whoever last created or changed the company: the user who created it, since nothing changes a company yet. |

`User`

| Field | Notes |
|-------|-------|
| `id` | UUIDv7. |
| `firstName`, `lastName` | A copy of the verified profile in Keycloak (ADR 0006, section 5). |
| `email` | `EmailAddress`: lower case. |
| `jobRole` | `RECRUITER`, `HIRING_MANAGER`, `HR`, `FOUNDER_OR_DIRECTOR`, `OTHER`. A profile field, not a right. |
| `companyId` | The company of the e-mail domain. |
| `membership` | `OWNER` for the first user of a company, `MEMBER` after that. Derived, never chosen. |
| `status` | `ACTIVE` (owner) or `PENDING_APPROVAL` (member). |

Rules the `User` aggregate owns:

- `create` decides membership and status from whether the company already existed. It takes an id generated up
  front (`User.newId()`), so a company created by the same user can record that user as `lastModifiedBy` before
  either is saved.

Input validation is not in the domain: the values come from an adapter that has validated them. `toString` of
`EmailAddress` and `User` is redacted, so personal data does not reach the logs.

## Block list

`identity/public-email-domains.txt` lists the public mail domains that never get an account, one per line with `#`
comments; an entry like `hotmail.*` matches the name under any top-level domain. The Keycloak realm enforces it on
every account (ADR 0006, section 3), and `KeycloakRealmTest` fails and prints the new pattern when the realm and the
file differ. The backend's own check moves behind a port with onboarding.

## Identity provider: Keycloak

Outbound port `IdentityProvider` (`application/port/out`), implemented by `KeycloakIdentityProvider`
(`adapter/out/keycloak`) on the realm's Admin REST API with Spring's `RestClient`. Not `keycloak-admin-client`: it pulls
in RESTEasy and Jackson 2, which the Enforcer bans.

| Operation | Keycloak call | Notes |
|-----------|---------------|-------|
| `getProfile(subject)` | `GET /users/{subject}` | Names, lower-cased e-mail address and whether Keycloak verified it. |
| `findOrCreateOrganization(domain, companyId)` | `POST /organizations`; on `409` `GET /organizations?search={domain}&exact=true` | Name and only domain = the e-mail domain, alias = the company id (ADR 0006, section 4). A `409` means an earlier attempt created it; the one with exactly that domain is returned. |
| `addOrganizationMember(organizationId, subject)` | `POST /organizations/{id}/members` with the subject as body | `409` (already a member) counts as success. Adds the account as an unmanaged member. |

- **Service-account token**: the `keycloak-admin` client registration (`client_credentials` as `talmar-backend`).
  `KeycloakAdminClientConfig` adds it to every call through Spring Security's `OAuth2ClientHttpRequestInterceptor`.
  One token for the whole backend, cached in Spring Boot's `OAuth2AuthorizedClientService` under a fixed principal
  until it expires; a token Keycloak rejects (`401` with `invalid_token`) is dropped and the next call fetches a new
  one. The token URI is configured, not discovered, so startup never calls Keycloak.
- **Timeouts**: the Admin API client and the token client are both built from Spring Boot's `RestClient.Builder`, so
  `spring.http.clients.*` applies. Spring Security's default token client has no timeout.
- **Validation** (`KeycloakProfileValidator`): a profile must fit Talmar's columns: names 1–35 characters from the
  realm's whitelist, e-mail address at most 75 characters with a dotted domain. The realm enforces the same rules;
  a profile that breaks them means the realm configuration changed.
- **Failures**: everything becomes `IdentityProviderUnavailableException`, naming the operation only. A timeout,
  connection error or `5xx` is logged at `warn` (a retry may help), any other unexpected answer, an invalid profile
  included, at `error`. Logs carry the HTTP status, never a response body: Keycloak may echo names or addresses in it.
- **Rights**: the service account has `manage-users` and `manage-organizations` (ADR 0006, section 6).

## Persistence

| Table | Entity | Notes |
|-------|--------|-------|
| `company` | `CompanyJpaEntity` | `email_domain` unique. `last_modified_by` not null. All fields `@ProcessData`. |
| `app_user` | `UserJpaEntity` | `user` is reserved in SQL Server. `email` unique. `company_id` is a foreign key to `company` (`fk_app_user_company`), mapped as a lazy `@ManyToOne`; the adapter sets it with `getReferenceById`, without a query. Index on `company_id`. |

- GDPR: `first_name`, `last_name` and `email` are `@PersonalData`. `job_role`, `membership`, `status` and
  `last_modified_by` are `@ProcessData`, following the ADR 0003 table. `company_id` is an association and the company
  classifies its own columns.
- `company.last_modified_by` holds the user id without a foreign key, so deleting a user never fails on, or rewrites,
  the audit trail of a company (ADR 0003, "Revisions").
- The removed `user_credentials` table and `verification_token_*` columns are not dropped by `ddl-auto: update`. A
  local database still has them until it is reset.

## Contract

`talmar-contracts/.../IdentityApi.yml`, generated by a second `openapi-generator-maven-plugin` execution into
`eu.talmar.api.identity.adapter.in.rest` and `eu.talmar.api.identity.adapter.in.web.contract`. It has no operations
yet; the `JobRole` and `AccountStatus` schemas stay for onboarding.

The execution differs from the vacancy one in two ways:

- **`schemaMappings` reuses the generated vacancy `ProblemDetails`.** The schema is identical, and
  `GlobalExceptionHandler` already answers every controller with that class. A second copy would also need its own
  SpotBugs exclusion, because the generated `List` accessors are flagged.
- **`generateSupportingFiles` is off.** Both executions write to the same output directory, so the identity run would
  overwrite the vacancy run's `ApiUtil` and `EnumConverterConfiguration`. Because of this the generated identity
  packages have no `package-info.java` with `@NullMarked`. A generated `ProblemDetailsErrorsInner` is left over and
  unused.

## Known gaps

- Spring Boot's `UserDetailsServiceAutoConfiguration` is still active. It creates an unused in-memory user and logs
  "Using generated security password" at startup. No login mechanism is enabled, so nothing accepts it. It goes away
  with the resource server.
- Two simultaneous first users of the same new domain will hit the unique constraint on `email_domain`; the second
  one would get a `500` (ADR 0006, section 7).
- There is no optimistic locking (`@Version`) on `company` or `app_user`.
- `PENDING_APPROVAL` users stay that way: owner approval does not exist yet.
