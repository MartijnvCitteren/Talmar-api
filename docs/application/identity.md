# Identity

Accounts and companies. Keycloak owns accounts, credentials, e-mail verification and login (ADR 0006); Talmar keeps
its own `company` and `app_user` and everything that decides access to company data.

This page covers what is built today: onboarding a logged-in Keycloak account (`POST /onboarding`), the domain
model, its persistence and the Keycloak adapter. The backend's own registration flow (ADR 0003) has been removed.
`GET /me`, profile editing, owner approval and securing the other endpoints do not exist yet.

## Domain model

Two aggregates in `domain/identity`, linked by id only in the domain (a user holds `companyId`). In the database
`app_user.company_id` is a foreign key to `company`: one company, many users.

`Company`

| Field | Notes |
|-------|-------|
| `id` | UUIDv7. |
| `name` | Derived from the e-mail domain when the company is created: the registrable label, capitalised (`acme.nl` and `sales.acme.co.uk` → "Acme"; `co`, `com`, `org`, `net`, `ac`, `gov`, `edu` before the top-level domain are skipped). There is no endpoint to correct it yet. |
| `emailDomain` | Lower case, unique. The key for "same company". |
| `organizationId` | The company's Keycloak organization; `null` until onboarding has linked it (`isLinkedToOrganization`). |
| `lastModifiedBy` | Required. The `app_user` id of whoever last created or changed the company: the user who created it, or the user whose onboarding linked the organization. |

`User`

| Field | Notes |
|-------|-------|
| `id` | UUIDv7. |
| `subject` | The Keycloak account's id, the `sub` claim of its access tokens. Unique. |
| `firstName`, `lastName` | A copy of the verified profile in Keycloak (ADR 0006, section 5). |
| `email` | `EmailAddress`: lower case. |
| `jobRole` | `RECRUITER`, `HIRING_MANAGER`, `HR`, `FOUNDER_OR_DIRECTOR`, `OTHER`. A profile field, not a right. |
| `companyId` | The company of the e-mail domain. |
| `membership` | `OWNER` for the first user of a company, `MEMBER` after that. Derived, never chosen. |
| `status` | `ACTIVE` (owner) or `PENDING_APPROVAL` (member). |
| `organizationMember` | Whether Keycloak has made the user a member of the company's organization. Only an owner becomes one at onboarding. |

Rules the `User` aggregate owns:

- `create` decides membership and status from whether the company already existed. It takes an id generated up
  front (`User.newId()`), so a company created by the same user can record that user as `lastModifiedBy` before
  either is saved.
- `needsOrganizationMembership` is true for an owner who is not an organization member yet;
  `markOrganizationMember` records that Keycloak added them.

`Company.linkOrganization` stores the organization id and records who linked it.

Input validation is not in the domain: the values come from an adapter that has validated them. `toString` of
`EmailAddress` and `User` is redacted, so personal data does not reach the logs.

## Onboarding — `POST /onboarding`

The first call after the first login (ADR 0006, section 2). Requires an access token; the body only has `jobRole`.
Names and e-mail address come from Keycloak, never from the request.

`OnboardUserService`, idempotent (ADR 0006, section 7):

1. Look up the user by the token's `sub`. If it exists and onboarding is complete (company linked, and an owner is an
   organization member) → `UserAlreadyOnboardedException`, `409`. Nothing changes.
2. If there is no user: fetch the profile from Keycloak. An unverified address, or one on a public mail domain
   (`PublicEmailDomains`), → `OnboardingNotAllowedException`, `403`; nothing is saved. Otherwise, in one transaction
   (`TransactionOperations`): find the company of the domain or create it, and save the user (`ACTIVE` owner or
   `PENDING_APPROVAL` member).
3. If the company is not linked: `findOrCreateOrganization`, then save the link. This belongs to the company: a
   colleague whose onboarding comes first links it for a founder whose onboarding stopped.
4. If the user is an owner without membership: `addOrganizationMember`, then save `organizationMember`.
5. Return the status, `200`.

No transaction is open during a Keycloak call. A Keycloak failure in any step is `503`
(`IdentityProviderUnavailableException`). What was saved before it stays, and the next call resumes at the step that
failed: a retry after step 2 does not fetch the profile again. A `PENDING_APPROVAL` member is not added to the
organization; that is for owner approval.

The public-domain check repeats what the realm already enforces (ADR 0006, section 3, layer 2), so one mistake in the
realm configuration does not let such an address create or join a company.

## Block list

`identity/public-email-domains.txt` lists the public mail domains that never get an account, one per line with `#`
comments; an entry like `hotmail.*` matches the name under any top-level domain. The Keycloak realm enforces it on
every account (ADR 0006, section 3), and `KeycloakRealmTest` fails and prints the new pattern when the realm and the
file differ. Onboarding checks it again through the `PublicEmailDomains` port, implemented by
`ClasspathPublicEmailDomains` (`adapter/out/blocklist`), which reads the file once at startup.

## REST adapter and security

- `OnboardingController` implements the generated `OnboardingApi`. `AuthenticatedUser.subject()` reads the `sub` of
  the validated token from the security context; the request mapper combines it with `jobRole` into the command.
- `IdentityExceptionHandler` (highest precedence) maps `OnboardingNotAllowedException` to `403`,
  `UserAlreadyOnboardedException` to `409` and `IdentityProviderUnavailableException` to `503`, as problem details with
  fixed texts. Requests are not logged.
- What the responses reveal: onboarding only acts on the account in the token (`sub`), never on one named in the
  request, so `403` and `409` say something about the caller's own account and nothing about other accounts. `ACTIVE`
  versus `PENDING_APPROVAL` does tell whether the e-mail domain already has a company, but only to an account with a
  verified address at that domain.
- The backend is an OAuth2 resource server (`spring-boot-starter-security-oauth2-resource-server`, only `issuer-uri`).
  It checks signature, issuer and expiry with Keycloak's public keys, fetched on the first request and cached, so no
  call to Keycloak per request. `SecurityConfig` requires a token for `/onboarding` only. A missing, expired or
  forged token is `401` with an empty body and only the RFC 6750 challenge: `WWW-Authenticate: Bearer`, plus
  `error="invalid_token"` when a token was sent. Spring's default `error_description` (why the token was rejected) and
  `resource_metadata` link are left out; the reason is logged at `warn`.

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
| `company` | `CompanyJpaEntity` | `email_domain` unique. `last_modified_by` not null. `keycloak_organization_id` nullable and **not** unique: SQL Server allows only one `NULL` in a unique column, so two unlinked companies would block each other; Keycloak keeps the ids unique. All fields `@ProcessData`. |
| `app_user` | `UserJpaEntity` | `user` is reserved in SQL Server. `email` and `keycloak_subject` unique. `first_name` and `last_name` are `nvarchar` (`@Nationalized`): Keycloak accepts any script, and the default `varchar` collation silently turned `Ł` into `L` (found against SQL Server; H2 in the tests is Unicode, so no test catches a regression). `company_id` is a foreign key to `company` (`fk_app_user_company`), mapped as a lazy `@ManyToOne`; the adapter sets it with `getReferenceById`, without a query. Index on `company_id`. |

- GDPR: `first_name`, `last_name` and `email` are `@PersonalData`. `job_role`, `membership`, `status`,
  `organization_member`, `last_modified_by` and the Keycloak ids are `@ProcessData`, following the ADR 0003 table and
  ADR 0006 section 5: the ids are random and identify nobody without Keycloak. `company_id` is an association and the company
  classifies its own columns.
- `company.last_modified_by` holds the user id without a foreign key, so deleting a user never fails on, or rewrites,
  the audit trail of a company (ADR 0003, "Revisions").
- The removed `user_credentials` table and `verification_token_*` columns are not dropped by `ddl-auto: update`, and
  existing name columns stay `varchar`. A local database needs a reset.

## Contract

`talmar-contracts/.../IdentityApi.yml`, generated by a second `openapi-generator-maven-plugin` execution into
`eu.talmar.api.identity.adapter.in.rest` (`OnboardingApi`) and `eu.talmar.api.identity.adapter.in.web.contract`.
`bearerAuth` is the default security of the contract.

The execution differs from the vacancy one in two ways:

- **`schemaMappings` reuses the generated vacancy `ProblemDetails`.** The schema is identical, and
  `GlobalExceptionHandler` already answers every controller with that class. A second copy would also need its own
  SpotBugs exclusion, because the generated `List` accessors are flagged.
- **`generateSupportingFiles` is off.** Both executions write to the same output directory, so the identity run would
  overwrite the vacancy run's `ApiUtil` and `EnumConverterConfiguration`. Because of this the generated identity
  packages have no `package-info.java` with `@NullMarked`. A generated `ProblemDetailsErrorsInner` is left over and
  unused.

## Known gaps

- Two simultaneous first users of the same new domain hit the unique constraint on `email_domain`; the second one
  gets a `500` (ADR 0006, section 7). A retry then joins the company as member.
- When Keycloak is unreachable on the first request with a token, the resource server cannot fetch the issuer's
  configuration and answers `500` with Spring's default error body instead of a `503` problem. Once the keys are
  cached, a Keycloak outage only affects calls to the Admin API.
- A `401` has an empty body here, while `VacancyApi.yml` declares a problem body for its `401`. Align both when #103
  secures the vacancy endpoints.
- An account that is deleted and created again in Keycloak with the same address gets a new `sub`; onboarding then
  hits the unique `email` and answers `500`.
- There is no optimistic locking (`@Version`) on `company` or `app_user`.
- `PENDING_APPROVAL` users stay that way: owner approval does not exist yet.
