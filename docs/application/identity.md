# Identity

Accounts and companies. This page covers what is built today: registration with a work e-mail address and its
confirmation (ADR 0003). Login, tokens, `GET /me`, owner approval and securing the other endpoints do not exist yet.

## Domain model

Two aggregates in `domain/identity`, linked by id only in the domain (a user holds `companyId`). In the database
`app_user.company_id` is a foreign key to `company`: one company, many users.

`Company`

| Field | Notes |
|-------|-------|
| `id` | UUIDv7. |
| `name` | Derived from the e-mail domain when the company is founded: the registrable label, capitalised (`acme.nl` and `sales.acme.co.uk` → "Acme"; `co`, `com`, `org`, `net`, `ac`, `gov`, `edu` before the top-level domain are skipped). There is no endpoint to correct it yet. |
| `emailDomain` | Lower case, unique. The key for "same company". |
| `lastModifiedBy` | Required. The `app_user` id of whoever last created or changed the company: the founder, since nothing changes a company yet. |

`User`

| Field | Notes |
|-------|-------|
| `id` | UUIDv7; will become the `sub` claim. |
| `firstName`, `lastName` | As validated by the contract (`Registrant`). |
| `email` | `EmailAddress`: lower case; its shape is validated by the contract. |
| `jobRole` | `RECRUITER`, `HIRING_MANAGER`, `HR`, `FOUNDER_OR_DIRECTOR`, `OTHER`. A profile field, not a right. |
| `companyId` | The company of the e-mail domain. |
| `membership` | `OWNER` for the first user of a company, `MEMBER` after that. Derived, never chosen. |
| `status` | `PENDING_VERIFICATION` → `ACTIVE` (owner) or `PENDING_APPROVAL` (member). |
| `credentials` | `UserCredentials(passwordHash)`, Argon2id; `null` until confirmed. |
| `verificationToken` | `VerificationToken(tokenHash, expiresAt)`: SHA-256 of the raw token; `null` once confirmed. |

Rules the `User` aggregate owns:

- `register` decides membership from whether the company already existed and starts in `PENDING_VERIFICATION`. It
  takes an id generated up front (`User.newId()`), so a company founded by the same registration can record the
  founder as `lastModifiedBy` before either is saved.
- `confirm` only works while `PENDING_VERIFICATION`, with the matching token hash, before `expiresAt`. It sets the
  credentials, clears the token (single use) and moves to `ACTIVE` or `PENDING_APPROVAL`. Otherwise it throws
  `InvalidVerificationTokenException`, one message for unknown, used and expired tokens.
- `renewVerificationToken` replaces the token of an unconfirmed user; the old link stops working.

Input validation is not in the domain: the value objects (`EmailAddress`, `Password`, `Registrant`) trust the REST
adapter, which validates every request against the contract first. `toString` of `EmailAddress`, `Password`, `Registrant`, `UserCredentials`, `User` and the commands is redacted, so
personal data and secrets do not reach the logs.

## Implemented use cases

### Register — `POST /registration`

`RegisterUserService`:

1. In the REST adapter, bean validation on the generated contract model rejects a request that breaks the contract
   with `400` on the field, before the use case runs:
   - `firstName`, `lastName`: 1–35 characters; letters of any script with their combining marks, spaces, hyphens,
     apostrophes (`'` and `’`) and periods; at least one letter; no leading, trailing or double spaces.
   - `email`: at most 75 characters, `@Email`, and a pattern for one `@` and a domain with a dot.
   - `jobRole`: one of the enum values.
2. Still in the REST adapter, `WorkEmailAddressValidator` rejects an address on a public mail domain with `400` on
   `email`. "Work addresses only" is part of the contract (see the `email` description); only the block list lives
   outside the YAML.
3. Address unknown: the domain's company is looked up. If there is none, `Company.found` creates it with a derived
   name and the new user's id as `lastModifiedBy`, and the user becomes `OWNER`. Otherwise the user joins as
   `MEMBER`. A token is issued, the company (if new) and the user are saved and
   `RegistrationMailer.sendVerificationMail` gets the raw token.
4. Address known and still unconfirmed: a fresh token replaces the old one and a new verification mail goes out.
   The name and job role of the repeated request are ignored.
5. Address known and confirmed: `sendAccountExistsMail`, nothing is saved.

The response is always `202` with an empty body for a valid request, so it never reveals whether an address has an
account. That the company exists does leak, through `PENDING_APPROVAL` after confirmation; ADR 0003 accepts that.

`VerificationTokenIssuer` creates 32 bytes from `SecureRandom`, URL-safe Base64 without padding (43 characters).
Only the SHA-256 hash is stored. A token is valid for `talmar.identity.unconfirmed-retention`.

### Confirm — `POST /registration/confirm`

The contract enforces the password policy: 12 to 128 characters, no composition rules (NIST SP 800-63B). A
violation gives `400` on `password` from bean validation. Note that bean validation counts UTF-16 units, so a
password of astral-plane characters (emoji) can pass with fewer than 12 code points.

`ConfirmRegistrationService` checks the token before it hashes the password, so the slow Argon2 hash runs only for a
request that will succeed. It returns `200` with `{"status": "ACTIVE" | "PENDING_APPROVAL"}`. An unknown, used or
expired token gives `400` on `token`; the contract tells the frontend to offer a new registration, which mails a
fresh link. It is deliberately not a `401` (ADR 0003, "Revisions").

### Purge unconfirmed registrations (scheduled)

`UnconfirmedRegistrationPurgeJob` (`adapter/in/scheduling`) calls `PurgeUnconfirmedRegistrationsService` every
`talmar.identity.purge-interval`. The first run also waits one interval. The service deletes users still
`PENDING_VERIFICATION` whose token expired, and their company if no user is left. Because a repeated registration
renews the token, retention runs from the latest verification mail. Tests call the use case directly.

## Adapters

- **REST**: `RegistrationController` implements the generated `RegistrationApi`. The request mappers turn the
  validated strings into `EmailAddress` and `Password`. `IdentityExceptionHandler` (highest precedence) maps the two
  identity exceptions to `400` problem details naming the field. Requests are not logged.
- **Mail**: `LoggingRegistrationMailer` writes the confirmation link to the log at INFO, built from
  `talmar.identity.confirmation-url` plus `?token=…`. It names the user by id, never by e-mail address. An EU mail
  provider replaces it behind the same port; that is where recurring cost starts.
- **Password hashing**: `Argon2PasswordHasher` uses Spring Security's `Argon2PasswordEncoder` with RFC 9106's second
  recommended option: 64 MiB memory, 3 iterations, parallelism 4, 16-byte salt, 32-byte hash. About 200 ms and
  64 MiB per hash on a developer machine, so concurrent confirmations (and later logins) cost server memory. Argon2 needs BouncyCastle
  (`bcprov-jdk18on`), which Spring Boot does not manage; its version is `bouncycastle.version` in the root `pom.xml`.
- **Block list**: `WorkEmailAddressValidator` (REST adapter) reads `identity/public-email-domains.txt` once at startup. It has
  one domain per line and `#` comments. An entry like `hotmail.*` matches the name under any top-level domain. Extend
  the file to block more domains, no code needed.

## Persistence

| Table | Entity | Notes |
|-------|--------|-------|
| `company` | `CompanyJpaEntity` | `email_domain` unique. `last_modified_by` not null. All fields `@ProcessData`. |
| `app_user` | `UserJpaEntity` | `user` is reserved in SQL Server. `email` unique. `company_id` is a foreign key to `company` (`fk_app_user_company`), mapped as a lazy `@ManyToOne`; the adapter sets it with `getReferenceById`, without a query. Indexes on `company_id` and `verification_token_hash`. |
| `user_credentials` | `UserCredentialsJpaEntity` | Shares the user's primary key (`@MapsId`), fetched eagerly, saved and deleted by cascade. |

- GDPR: `first_name`, `last_name`, `email` and `password_hash` are `@PersonalData`. `job_role`, `membership`,
  `status`, the token columns and `last_modified_by` are `@ProcessData`, following the ADR 0003 table. `company_id`
  is an association and the company classifies its own columns.
- `company.last_modified_by` holds the user id without a foreign key. The purge can delete a founder whose colleague
  already joined; the company then keeps pointing at a user id that no longer exists. That is deliberate (ADR 0003,
  "Revisions"): the audit reference is never blocked or rewritten.
- The token hash has a plain index, not a unique constraint. SQL Server allows only one `NULL` in a unique column,
  and every confirmed user has a `NULL` hash.

## Contract

`talmar-contracts/.../IdentityApi.yml`, generated by a second `openapi-generator-maven-plugin` execution into
`eu.talmar.api.identity.adapter.in.rest` (`RegistrationApi`) and `eu.talmar.api.identity.adapter.in.web.contract`. Both
operations declare `security: []`.

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
  "Using generated security password" at startup. No login mechanism is enabled, so nothing accepts it. Remove it in
  the login slice.
- In production the confirmation link, which contains a working token, ends up in the application log until a real
  mailer replaces `LoggingRegistrationMailer`.
- The mail is sent inside the registration transaction. With a real mail provider, a failing send rolls back the
  registration (`500`), and a send followed by a failing commit mails a link that does not work.
- Two simultaneous first registrations for the same new domain hit the unique constraint on `email_domain`. The
  second one gets a `500`. The same applies to two simultaneous registrations of one address.
- There is no optimistic locking (`@Version`). Two simultaneous confirmations with the same token can both succeed,
  and the last password wins.
- Saving an already-confirmed user again would write `NULL` into `user_credentials.created_at`. The adapter maps a
  fresh credentials entity, and JPA auditing only fills `created_at` on insert. Today a confirmed user is never saved
  again. The vacancy description has the same pattern.
- A new address does more database work than an existing one, so response timing may hint at whether an address is
  registered. Not measured or mitigated.
- `PENDING_APPROVAL` users stay that way: owner approval needs login.
