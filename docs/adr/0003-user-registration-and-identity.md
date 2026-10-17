# ADR 0003 — User registration and identity in the backend

- **Status:** Accepted, revised 2026-10-11 after PR review (see "Revisions")
- **Date:** 2026-10-09

## Context

Epic 3 of the build order: accounts and login, so that vacancies belong to a company and company data can feed the
generated vacancy texts. This ADR covers the first slice, registration: a recruiter registers with their work
e-mail, confirms it through a link in an e-mail and sets a password. Login, token issuing and securing the existing
endpoints are the next slice; they are designed here only far enough to make sure registration does not block them.

Requirements and constraints:

- **The backend owns identity.** User data is sensitive and Talmar is heading towards enterprise customers; the
  user store, the credentials and the registration rules live in this repository, not in the frontend stack.
- **Spring Security, the current way**: stateless, JWT-based, `oauth2ResourceServer` style; no sessions.
- **Tokens carry IDs and rights only** (`sub`, `org`, `roles`). Name and e-mail are personal data and do not belong
  in something that ends up in every request log; the frontend fetches them from a `GET /me`.
- **Company comes from the e-mail domain.** The first person from a domain creates the company and becomes its
  owner; later registrants from the same domain join the existing company after the owner approves them. Public
  mail domains (gmail, outlook, proton, …) are not work domains and are rejected.
- **SSO later**, both social login and, for enterprise customers, per-organisation SAML/OIDC. Nothing of that is
  built now, but the model must not make it hard.
- **GDPR**: personal data and process data are separated per field, like everywhere in the code base.
- **EU only**: no external service outside the EU, including for mail or password-breach checks.

## Decision

### 1. Build identity in Spring, not with a third-party auth framework

**Better Auth was considered and rejected.** It is a TypeScript framework: it owns the `user`, `account`, `session`
and `verification` tables in a Node application, which would put identity with the frontend team and leave the
backend with a mirror of each user. Our registration flow (name and e-mail first, password only after the e-mail is
confirmed) is not its default flow and would be plugin work in Node. The hybrid (Better Auth issues JWTs, Spring only
validates) stays a valid option if the frontend stack ever needs it, because the backend validates JWTs the same way
either way.

**A self-hosted identity provider (Zitadel, Keycloak) was considered and deferred.** It is the strongest option for
per-customer enterprise SSO, but it is an extra container and database to run before there is a single customer.
The `User` model keeps credentials in a separate entity so that an external identity can sit next to, or instead of,
a password later, which is what a move to an IdP needs.

**Spring Authorization Server was considered and deferred** for the same reason: a full OAuth2/OIDC server is more
than a single SPA needs. The backend will issue its own JWTs with Spring Security's Nimbus support (next slice).

### 2. Domain model

Two aggregates in `domain/identity/`:

`Company`

| Field | Classification | Notes |
|-------|----------------|-------|
| `id` | – | UUIDv7. |
| `name` | `@ProcessData` | Derived from the e-mail domain (`acme.co.uk` → "Acme"); the users correct it later. |
| `emailDomain` | `@ProcessData` | Unique, lower-case. The key for "same company". |
| `lastModifiedBy` | `@ProcessData` | Required: the `app_user` id of whoever last created or changed the company; the founder at first. An id, no foreign key (see "Revisions"). |

`User`

| Field | Classification | Notes |
|-------|----------------|-------|
| `id` | – | UUIDv7; becomes the `sub` claim. |
| `firstName`, `lastName`, `email` | `@PersonalData` | `email` unique, lower-case. |
| `jobRole` | `@ProcessData` | Enum from the registration dropdown: `RECRUITER`, `HIRING_MANAGER`, `HR`, `FOUNDER_OR_DIRECTOR`, `OTHER`. A profile field, not a right. Classified as process data (maintainer decision): the values are too generic to identify a person on their own. |
| `companyId` | – | Foreign key to `company` (`@ManyToOne` in JPA): one company, many users. The domain keeps the id only. |
| `membership` | `@ProcessData` | `OWNER` or `MEMBER`; derived, never chosen in a form. First user of a company is `OWNER`. |
| `status` | `@ProcessData` | `PENDING_VERIFICATION` → `ACTIVE` (first user of the company) or `PENDING_APPROVAL` (later users) → `ACTIVE` once the owner approves. |
| `credentials` | `@PersonalData` | Separate entity: Argon2id password hash (64 MiB, 3 iterations, parallelism 4), set at confirmation. Separate so an external identity (SSO) can be added without touching the user row. |
| `verificationToken` | `@ProcessData` | SHA-256 of the token and its expiry; cleared on confirmation. |

Rules the aggregates own:

- A `User` can only be confirmed while `PENDING_VERIFICATION` and before the token expires; the token is single-use.
- Membership and status are decided by whether the company already existed when the user registered.
- Owner approval of a `PENDING_APPROVAL` user needs an authenticated owner and is therefore part of the login slice;
  the status exists now so those registrations are not lost.

### 3. Registration flow

1. `POST /registration` with `firstName`, `lastName`, `email`, `jobRole`.
   - The REST adapter validates the request against the contract before anything else runs: names 1–35 characters
     of letters, spaces, hyphens, apostrophes and periods; `email` at most 75 characters with a domain that contains
     a dot → otherwise `400` on that field.
   - The domain of `email` must not be on the public-domain block list (`identity/public-email-domains.txt` on the
     classpath; extendable without code) → otherwise `400` on `email`. A contract rule, so the REST adapter checks
     it, before the use case runs.
   - Domain unknown → create `Company(name derived from the domain, emailDomain, lastModifiedBy = the new user's
     id)`, user becomes `OWNER`.
   - Domain known → user joins that company as `MEMBER`.
   - Always `202`, also when the e-mail is already registered: an existing unconfirmed registration gets a fresh token
     and a new verification mail, a confirmed one gets a mail saying the account exists and inviting to log in. The
     response never reveals whether an e-mail address exists (enumeration). That the *company* exists is revealed (status `PENDING_APPROVAL` after confirmation); accepted.
   - A verification mail is sent through the `RegistrationMailer` port with a link to
     `talmar.identity.confirmation-url` plus the token.
2. `POST /registration/confirm` with `token` and `password`.
   - Password 12–128 characters, no composition rules (NIST 800-63B), checked by the contract; stored as Argon2id
     with RFC 9106's second recommended parameters (64 MiB, 3 iterations, parallelism 4). No breach-list check: it
     would be an external call per registration and the usual service is outside the EU.
   - Valid token → `ACTIVE` for an owner, `PENDING_APPROVAL` for a member; token cleared. Unknown, used or expired
     → `400` on `token`, one response for all three.
3. A `@Scheduled` purge deletes users still `PENDING_VERIFICATION` after `talmar.identity.unconfirmed-retention`
   (default `PT48H`), with their company if it has no other users. Same pattern as the generation purge in ADR 0002.

The mail adapter for now is `LoggingRegistrationMailer` (writes the link to the log). An EU mail provider goes behind
the same port later; that is the point where recurring cost starts.

### 4. Security configuration

Nothing is secured in this slice; every endpoint stays open, as today. The login slice adds `SecurityFilterChain`
with `oauth2ResourceServer().jwt()`, backend-issued JWTs (short-lived access token with `sub`, `org`, `roles`; refresh
token in an http-only cookie), `GET /me`, the owner-approval endpoint and the closing of `/vacancy`. Social login is
then `oauth2Login()` configuration; per-customer enterprise SSO is the trigger to revisit an IdP.

### 5. Contract

A separate `IdentityApi.yml` next to `VacancyApi.yml`, generated into `eu.talmar.api.identity.adapter...` by a
second execution of the generator plugin. Identity has a different consumer lifecycle and, soon, different security
than the vacancy API. The existing `bearerAuth` scheme in `VacancyApi.yml` stays as it is.

## Consequences

- New tables: `company`, `app_user` (`user` is reserved in SQL Server), `user_credentials`, with a foreign key from
  `app_user.company_id` to `company`. `BaseJpaEntity.modifiedBy` (a string) becomes `lastModifiedBy` (a user id,
  `@ProcessData`) for every table. `schema-change` label.
- A second codegen execution in `talmar-api/pom.xml` (`<build>` change → `guardrail-change-ok`).
- `spring-boot-starter-security` enters the dependency tree now (Argon2 comes with it). The starter secures everything
  by default, so this slice ships a `SecurityFilterChain` that explicitly permits all requests and disables CSRF and
  form login, so behaviour does not change before the login slice. Having the chain in place now means the login
  slice only tightens configuration and the tests already run through the security filters.
- New configuration: `talmar.identity.confirmation-url`, `talmar.identity.unconfirmed-retention`.
- The frontend gets a registration form, a confirmation page that reads the token from the URL, and must treat
  `202` as "check your mail" regardless of whether the address was new.
- Delivered as one PR for registration and confirmation (contract, domain, persistence, use cases, mail port, purge,
  docs); login and securing endpoints are a separate PR series with its own ADR if the design here needs revision.

## Revisions

Changed on 2026-10-11 after the maintainer's review of the first PR. The superseded choices and why they lost:

- **Company name in the registration form → derived from the e-mail domain.** The form asked for `companyName`,
  used only by the first registrant of a domain and silently ignored for everyone after. That is a field most users
  fill in for nothing, and the domain is the company key anyway. The name now starts as the registrable label of the
  domain, capitalised; correcting it is a later feature for the company's users.
- **Validation in the domain → validation at the adapter.** The first version checked null, blank, length, e-mail
  shape and the password policy in the value objects (`EmailAddress`, `Password`, `Registrant`). Those are contract
  rules: they belong in `IdentityApi.yml`, where bean validation rejects a bad request before the application layer
  runs and the frontend sees the same rules. The domain keeps the business rules (membership, token expiry).
- **Free-text names (1–100, any non-blank text) → 1–35 characters from a whitelist.** 35 is the length the UK
  Government Data Standards Catalogue uses for given and family names. There is no international standard for the
  characters in a name (ICAO 9303 only covers the transliterated passport zone); following the W3C guidance on
  personal names, any letter of any script is allowed, plus the punctuation real names use (`'t Hooft`, `O’Brien`,
  `Anne-Marie`, `St. John`). Digits and symbols are rejected. E-mail is limited to 75 characters instead of RFC
  5321's 254.
- **Argon2id at the OWASP baseline (19 MiB, 2 iterations, parallelism 1) → RFC 9106's second recommended option
  (64 MiB, 3 iterations, parallelism 4).** The baseline is the least OWASP accepts; the stronger setting costs about
  200 ms and 64 MiB per hash, which is affordable at our volume. RFC 9106's first option (2 GiB per hash) is not, on
  a small container. Hashing endpoints need rate limiting once login exists.
- **User and company linked by id only → a foreign key.** The database now guards the one-to-many relation between
  `company` and `app_user` (`@ManyToOne` in JPA). The domain still references the company by id: they stay separate
  aggregates.
- **Who changed a company was not recorded → `company.last_modified_by` holds the `app_user` id, required.** It has
  no foreign key on purpose: the purge deletes unconfirmed users, including a founder whose colleague already joined,
  and a foreign key would block that delete or force the audit reference to be rewritten. An id without personal
  data also survives anonymization, like the rest of the audit trail.
- **Public mail domain check in the use case → in the REST adapter** (2026-10-15, second review). The first
  revision kept it in `RegisterUserService` behind a `PublicEmailDomains` port, arguing "work addresses only" is a
  business rule. It is an API requirement instead: the contract already promises it, and when it changes, the
  adapter should be the only place to change. `WorkEmailAddressValidator` now reads the block list and rejects the
  request before the use case runs; the port is gone.
- **`401` for an expired confirmation token was considered and rejected.** The token is not an authentication
  credential, `401` requires a `WWW-Authenticate` challenge, and once login exists the frontend will treat `401` as
  "access token expired". The frontend gets `400` with an error on `token` and offers to register again, which mails
  a fresh link. Unknown, used and expired stay indistinguishable; with the purge, an expired registration becomes an
  unknown one within an hour anyway.
