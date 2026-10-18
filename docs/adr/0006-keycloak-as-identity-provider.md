# ADR 0006 — Keycloak as identity provider

- **Status:** Accepted
- **Date:** 2026-10-18
- **Numbering:** 0006, not 0004: comments in the vacancy code cite an ADR 0004 and 0005 that are not in `docs/adr/`
  (known gap in `overview.md`). Reusing those numbers would make those comments point at this ADR.
- **Supersedes:** ADR 0003 decision 1 (identity built in Spring), the credentials, verification token and
  `PENDING_VERIFICATION` parts of decision 2, decision 3 (registration flow) and the backend-issued JWTs of decision 4.
  The rest of ADR 0003 stands: the company comes from the e-mail domain, owner and member rules, the GDPR
  classification, the separate `IdentityApi.yml`.

## Context

ADR 0003 built registration in the backend: `POST /registration` and `POST /registration/confirm`, Argon2id hashes in
`user_credentials`, verification tokens, a purge job and a logging mailer. It deferred a self-hosted identity provider
"until there is a reason". There are now two:

- **Per-customer SSO.** Enterprise customers will want to log in with their own SAML/OIDC provider. Building
  federation, login, refresh tokens, password reset, brute-force protection and MFA in Spring is a product of its own.
- **Not holding passwords.** Every password hash we store is a liability we have to protect, rate-limit and
  eventually migrate.

Constraints that shaped this decision:

- **EU only, self-hosted.** No US-owned identity service, not even in an EU region (`.claude/CLAUDE.md`, hosting).
- **The business rules stay in Talmar**: the company comes from the e-mail domain, the first registrant of a domain
  founds it and becomes `OWNER`/`ACTIVE`, later registrants join as `MEMBER`/`PENDING_APPROVAL`. The identity
  provider stores accounts; it does not decide who belongs to which company.
- **No public mail domain ever gets an account** (maintainer requirement, 2026-10-18). Not "no Talmar user": no
  login at all, so nobody with a gmail address can authenticate against anything of ours.
- **Tokens carry ids and rights only** (ADR 0003): no name or e-mail address in an access token.
- **Build constraints**: the Maven Enforcer bans Jackson 2, and new integrations go behind a port.
- **Local only for now.** Production hosting is not decided; nothing here may tie us to one provider.

## Decision

### 1. Keycloak, self-hosted, owns accounts and credentials

Keycloak 26.8 (pinned to an exact version) runs as a container next to the application. It owns accounts,
passwords, e-mail verification, login, tokens, password reset and brute-force protection. Talmar keeps its own
`app_user` and `company` and everything that decides access to company data.

The minimum version is 26.7: it introduced the `manage-organizations` admin role (section 6). Before that, managing
organizations required `manage-realm`, which can change every realm setting.

**Alternatives considered:**

- **Keep the backend's own flow (ADR 0003) and issue JWTs with Nimbus.** Rejected: we keep storing passwords, and
  SSO, login, refresh tokens, password reset and brute-force protection all remain ours to build. That is the work
  ADR 0003 said would trigger an identity provider.
- **Zitadel.** Strong multi-tenancy model, but it supports PostgreSQL only, so it needs a second database engine next
  to SQL Server (v3 dropped CockroachDB). The server moved to AGPL-3.0 in v3: running it unmodified is fine, but a
  change to the server, such as a custom validator for section 3, would have to be published. Keycloak runs on SQL
  Server (section 8), is Apache 2.0 and has far more Spring documentation and community knowledge.
- **Spring Authorization Server.** Rejected for the same reason as the own flow: it issues tokens, but registration,
  verification, user management, an admin UI and SSO federation are still ours to build.
- **Better Auth.** Rejected in ADR 0003 (identity in the Node frontend stack); nothing changed.
- **A managed identity service (Auth0, Okta, Cognito, Entra ID).** Excluded: US jurisdiction.

### 2. Registration and onboarding flow

1. The user registers on the Keycloak registration page (e-mail address, first name, last name) and verifies the
   address through the mail Keycloak sends. Only then does Keycloak ask for a password: with e-mail verification on,
   it leaves the password fields off the registration form and adds the `UPDATE_PASSWORD` required action instead.
   That is the order ADR 0003 designed (password after confirmation), and an unverified account has no password.
   Keycloak issues no token before the address is verified.
2. The frontend logs in with authorization code + PKCE (client `talmar-web`) and calls `POST /onboarding` with the
   job role.
3. The backend takes `sub` from the token and fetches the verified profile (names, e-mail address) from the Keycloak
   Admin API. The access token does not carry them (ADR 0003).
4. The existing domain rules decide founder or member. A new company gets a Keycloak organization (section 4) and the
   founder becomes a member of it. A `PENDING_APPROVAL` member is **not** added to the organization until the owner
   approves them (a later story), so they cannot reach company data.

Contract of `POST /onboarding` (`IdentityApi.yml`, `bearerAuth`):

| Response | When |
|----------|------|
| `200` `{ status: ACTIVE \| PENDING_APPROVAL }` | Onboarded, or a retry finished an interrupted onboarding (section 7). |
| `400` | Body breaks the contract (missing or unknown `jobRole`). |
| `401` | No, expired or wrongly signed token. |
| `403` | The verified profile is not allowed to onboard: the e-mail address is on a public mail domain, or not verified. |
| `409` | Already onboarded; nothing changes. |
| `503` | Keycloak unavailable or failing; the company and user may be saved, a retry finishes the rest. |

**Why `403` and not the story's `400` on `email`:** the request is well-formed and has no `email` field; what fails
is who the authenticated user is. A `400` naming a field the client never sent is misleading. In practice this
response should never happen, because Keycloak already refuses such an account (section 3); it is the second line.

The job role is asked by Talmar after the first login, not on the Keycloak registration form. A custom user attribute
in Keycloak would make it a Keycloak concern and need a second copy anyway; it is a Talmar profile field.

The old endpoints `POST /registration` and `POST /registration/confirm` are removed without a deprecation period
(maintainer decision, 2026-10-18): they shipped one week earlier and nothing depends on them. A deprecation period
would also keep storing passwords until the removal.

### 3. Public mail domains never get an account

Two layers, so a mistake in one does not let an account through.

**Layer 1 — Keycloak user profile.** The realm's user profile gets a `pattern` validator on `email` that rejects
every domain from the block list, with a readable error message on the registration form. Keycloak applies the same
user-profile validation in every context in which a user is created or changed: registration, update profile, the
review step after a social or SSO login, the account console, and the Admin API and admin console. So this one rule
also covers accounts created by an administrator, invited organization members and, later, federated SSO users.

- `talmar-api/src/main/resources/identity/public-email-domains.txt` stays the single source of the list.
  `KeycloakRealmTest` computes the pattern from the list and compares it with the realm file; when they differ, the
  build fails and prints the pattern to paste in, so the two cannot drift apart. The pattern writes a dot as `[.]`, so
  it contains no backslashes and needs no escaping inside the JSON string.
- An entry like `hotmail.*` keeps its meaning (any top-level domain); the regex is case-insensitive and anchored on
  the domain part of the address.

**Layer 2 — onboarding.** `OnboardUserService` checks the e-mail domain of the verified profile against the same list
through a port (`PublicEmailDomains`) and answers `403`; nothing is saved. ADR 0003's revision of 2026-10-15 moved
this check from the use case to the REST adapter because the address was request input. It no longer is: it comes
from the identity provider, and "only work domains found or join a company" is now a rule of the onboarding use case.
The port returns, implemented by an adapter that reads the same file.

**Other ways in, closed:**

- E-mail verification is required before login, so a typo or someone else's address cannot be used.
- Users cannot change their e-mail address in Keycloak after registration (section 5).
- No social login or other identity providers are configured in the realm.

**Alternative considered — a custom Keycloak validator (Java extension).** It plugs into the same user-profile
validation, so it blocks nothing the pattern validator does not. It costs a separate build outside the reactor
(Keycloak's server libraries pull in Jackson 2, which the Enforcer bans), a custom Keycloak image, and a re-test on
every Keycloak upgrade because the validator interface is not a public API. Rejected for a list of about 75 entries;
reconsider when the list grows to hundreds of entries (e.g. disposable-mail domains) or needs logic a regex cannot
express (e.g. an MX lookup).

**Limit:** a domain added to the list later does not remove existing accounts on it. That is a manual clean-up.

### 4. A Keycloak organization per company

Keycloak organizations are the multi-tenancy model the identity provider offers: membership is what the next story
(#103) puts in the token to scope the API to a company, and per-customer SSO is configured per organization.

| Organization field | Value | Why |
|--------------------|-------|-----|
| Name | The company's e-mail domain (`acme.nl`) | Keycloak requires a unique name. The derived company name is not unique: `acme.nl` and `acme.de` both become "Acme", and the second company would fail to provision. The domain is unique by construction. |
| Alias | The Talmar company id | Unique, URL-friendly, and ties the organization to the company without a lookup. |
| Domain | The company's e-mail domain, exact | Not a wildcard: `sales.acme.nl` is a separate Talmar company today (ADR 0003). Keycloak does not let two organizations share a domain, which makes the domain a safe key for the retry lookup (section 7). |

- Members are added as existing realm users: **unmanaged** members. Removing a member or deleting the organization
  never deletes the Keycloak account.
- Keycloak does **not** check a member's e-mail domain when the Admin API adds a member: in PR 1 a `globex.com`
  user was accepted into the `acme.nl` organization. The onboarding use case only ever adds a user to the
  organization of their own company, so that rule lives in Talmar alone.
- **Side effect:** once a realm has at least one organization, Keycloak's browser flow becomes identity-first: the
  login page asks for the e-mail address first and the password on a second screen. Without any organization the
  login page is the usual single form. This affects login page branding
  (#104) and what the frontend team should expect.

### 5. Personal data: a copy in `app_user`

First name, last name and e-mail address are copied from Keycloak into `app_user` at onboarding, still
`@PersonalData`. Keycloak is the source for login; the copy is what Talmar shows and uses.

- **Why a copy:** `GET /me` and later features (e.g. the recruiter as contact person in a vacancy text) read it
  without calling Keycloak, so the backend stays a stateless resource server that does not depend on Keycloak being
  up for reads.
- **Alternative considered — read from Keycloak on demand.** One copy of the personal data, but every profile read
  becomes an Admin API call, a Keycloak outage takes down those reads, and the service account needs user-read rights
  at runtime. Rejected.
- **Drift:** the e-mail address is the company key, so users cannot change it in Keycloak. Users cannot edit their
  names in Keycloak either; they change them through Talmar (`PATCH /me`, see "Revisions"), which writes through to
  Keycloak, so the copy cannot drift. How the lock works (each way tested in PR 1):
  - **E-mail address:** it is the username, and username editing is off. Keycloak marks it read-only for the user
    and ignores a change sent to the account API. Nothing extra is needed.
  - **Names:** user-profile permissions cannot do it. With "edit: admin only", Keycloak also leaves the field off the
    registration form, so nobody could register. Instead, two settings close both ways a user can edit their
    profile:
    - `manage-account` is removed from the realm's default roles, which switches off the account console and its
      REST API (`401`). Password changes go through "forgot password".
    - The `UPDATE_PROFILE` required action is disabled. It also serves application-initiated requests
      (`kc_action=UPDATE_PROFILE`), which anyone can start by building the URL; PR 1 changed a name that way before
      it was disabled.
  - The realm import cannot express either setting: Keycloak re-adds `manage-account` when it creates the `account`
    client, and a partial `requiredActions` list replaces Keycloak's defaults (dropping `VERIFY_EMAIL`). A one-shot
    `kcadm.sh` script applies both after every start (section 8).
  - **Remaining way:** `VERIFY_PROFILE` stays enabled. It shows the profile form only when a stored profile fails the
    realm's rules, for example after an administrator makes the rules stricter. A user cannot cause that alone.
    Disabling it would lock out users whose profile has become invalid.
- **GDPR:** personal data now lives in two places. Anonymization (a later feature) must also delete the Keycloak
  user; the identity-provider port gets that operation when anonymization is built. `app_user.keycloak_subject` and
  `company.keycloak_organization_id` are `@ProcessData`: random ids that identify nobody without Keycloak.

### 6. Backend integration

- **Resource server:** `spring-boot-starter-security-oauth2-resource-server` with only `issuer-uri`. The backend validates
  token signatures with Keycloak's public keys and never calls Keycloak per request. In this story only
  `/onboarding` requires a token; securing every endpoint, `GET /me` and the token claims are #103.
- **Admin API:** outbound port `IdentityProvider` (fetch the verified profile by subject, create an organization,
  find an organization by domain, add a member), implemented in `adapter/out/keycloak` with Spring's `RestClient`.
  The service-account token comes from `spring-boot-starter-security-oauth2-client` (`client_credentials`), cached until it
  expires.
  - **Not `keycloak-admin-client`:** it pulls in RESTEasy and Jackson 2, which the Enforcer bans.
  - The adapter validates what Keycloak returns before it reaches the domain: e-mail address verified, at most 75
    characters, names 1–35 characters from the contract's whitelist. The realm enforces the same rules (section 8);
    the adapter guards our columns if the realm configuration ever changes.
  - Failures: a timeout, connection error or `5xx` is temporary, and the user can retry, which section 7 makes safe.
    Any other unexpected status is our bug or a misconfiguration and is logged at `error`. Both answer `503` without
    internal details. No automatic retry inside the request.
- **Service account `talmar-backend`**, realm-management roles:

  | Role | Needed for |
  |------|------------|
  | `manage-organizations` | Creating organizations and adding members; includes viewing and searching organizations. |
  | `manage-users` | Adding a member checks *manage* permission on that user (read in the Keycloak 26.8 source), and reading the profile needs at least `view-users`, which `manage-users` includes. |

  **Cost:** `manage-users` lets the backend change or delete any user in the `talmar` realm, including resetting
  passwords. A leaked client secret is therefore serious: the secret only comes from an environment variable and is
  rotated on suspicion. Keycloak's fine-grained admin permissions (v2) can narrow this to "manage members" later; not
  done now because they add a policy model to configure and test for one call.

### 7. Consistency: onboarding is idempotent

Keycloak and the Talmar database cannot share a transaction, so onboarding is built to be repeated until it is
complete. No database transaction is held open during a Keycloak call.

1. Look up the user by `keycloak_subject`. If the user exists and onboarding is complete → `409`.
2. If the user does not exist: fetch the profile, apply the public-domain rule, then save the company (if new) and the
   user in one transaction.
3. If the company has no `keycloak_organization_id`: create the organization. If Keycloak answers `409`, an earlier
   attempt created it but did not save its id: find it by exact domain and link that one. Save the id.
4. If the user is an `OWNER` and not yet an organization member: add them. A `409` from Keycloak ("already a member")
   counts as success. Save that the user is a member.
5. A Keycloak failure in step 3 or 4 → `503`. The next `POST /onboarding` resumes at the step that failed.

"Onboarding is complete" means: the user exists, the company is linked to an organization, and an owner is a member.
Storing membership in `app_user` (a process-data flag) makes that check a database read; the approval story sets it
for a member.

Step 3 belongs to the company, not to the founder: if a founder's onboarding stopped after step 2 and a colleague
onboards first, the colleague's request creates and links the organization (but does not add the colleague).

**Not solved:** two first users of one new domain onboarding at the same moment hit the unique constraint on
`email_domain`; the second gets a `500`. This known gap of ADR 0003 stays and is listed in `identity.md`.

### 8. Local setup and realm configuration

- **Docker Compose:** Keycloak with `start-dev --import-realm`, `KC_DB=mssql` on a `keycloak_db` database in the
  existing SQL Server container. `sqlserver-init` creates it with:
  - a UTF-8 collation, because names may use any script and Keycloak warns about anything else;
  - `READ_COMMITTED_SNAPSHOT` on, as Keycloak recommends.

  Mailpit is the local SMTP server, with a web UI for the verification mails. `docker compose up` needs no manual
  steps.
- **Realm file** in `docker/keycloak/` (not `config/`, which holds guardrail files). Keycloak imports it on the
  first start only, so a change needs a realm reset locally. The `talmar-backend` client secret is a `${…}`
  placeholder resolved from an environment variable, which Keycloak's import supports. The committed default is a
  low-entropy, local-only value.
- **`keycloak-config`:** a one-shot container on the Keycloak image that runs `docker/keycloak/configure-realm.sh`
  (`kcadm.sh`) after Keycloak is healthy, for the two settings the import cannot express (section 5). It is
  idempotent, so it runs after every start.
- **Realm `talmar`:** self-registration on, the e-mail address is the username, login with e-mail, no duplicate
  e-mail addresses, e-mail verification required, forgot password on, brute-force protection on, organizations on,
  unmanaged user attributes disabled.
- **User profile:** the contract's former `Registrant` rules move here. First and last name 1–35 characters with the
  same character whitelist; e-mail address at most 75 characters plus the block list (section 3).
- **Password policy:** `length(12)`, `maxLength(128)`, not equal to the e-mail address, no composition rules (NIST SP
  800-63B), as ADR 0003.
- **Password hashing:** Keycloak hashes with Argon2 by default, but with 7 MiB memory, 5 iterations and parallelism 1.
  We configure RFC 9106's second recommended option, as ADR 0003 did: 64 MiB, 3 iterations, parallelism 4. Cost:
  about 64 MiB per login or registration being processed at the same time, now on the Keycloak container.
- **Clients:**
  - `talmar-web`: public, authorization code only, PKCE `S256` required, redirect URIs and web origin for the local
    frontend (`http://localhost:5173`). No implicit flow, no direct access grants.
  - `talmar-backend`: confidential, service account only (no login flows), roles from section 6.
- **Database for Keycloak:** Keycloak lists SQL Server 2019 and 2022 as supported; our container runs 2025.
  Keycloak 26.8.1 creates its schema and runs on it locally (PR 1). Because 2025 is not on Keycloak's tested list, a
  Keycloak upgrade can break it; the fallback is a separate PostgreSQL container for Keycloak only. The production
  database for Keycloak is decided with the hosting provider.

## Consequences

- **Removed:** both registration endpoints, `RegistrationController` and its mappers, `WorkEmailAddressValidator`
  (the list moves behind the `PublicEmailDomains` port), `Password`, `Registrant`, `UserCredentials`,
  `VerificationToken`, `PENDING_VERIFICATION`, `InvalidVerificationTokenException`, `Argon2PasswordHasher` and
  BouncyCastle, `RegistrationMailer` and `LoggingRegistrationMailer`, the unconfirmed-registration purge and
  `talmar.identity.*` properties. The three known gaps they caused (confirmation link in the log, mail inside the
  transaction, credentials `created_at`) disappear with them.
- **Schema change** (`schema-change` label): `user_credentials` and the token columns are dropped; `app_user` gets
  `keycloak_subject` (unique) and the organization-member flag; `company` gets `keycloak_organization_id` (unique,
  nullable until linked). With `ddl-auto: update`, a local database needs a reset.
- **Breaking contract change**: the registration operations are removed; `oasdiff` flags it, the maintainer accepted
  it (section 2).
- **New dependencies:** `spring-boot-starter-security-oauth2-resource-server`,
  `spring-boot-starter-security-oauth2-client` (Spring Boot 4 names; the old `spring-boot-starter-oauth2-*` names
  are deprecated).
- **New configuration:** issuer URI, Admin API base URL, client id and secret, all from environment variables.
- **Running cost:** one more container to host and keep patched, roughly 1 GB of memory plus its database, and the
  Argon2 memory per concurrent login. No licence or per-user cost. The image is the standard Keycloak image, so any
  EU provider that runs containers can host it.
- **Frontend:** an OIDC client library (authorization code + PKCE), an onboarding screen with the job role after the
  first login, the identity-first login screens of section 4, and a `503` on onboarding means "try again".
- **Unverified accounts are kept.** ADR 0003 purged unconfirmed registrations; Keycloak keeps an unverified
  account (without a password) indefinitely. Someone who registers another person's address first blocks that
  person's registration ("already associated with an existing account") until the real owner uses "forgot
  password", which proves the address. A clean-up of old unverified accounts is a later task.
- **No account console.** Users cannot sign out other sessions or set up two-factor authentication themselves. That
  comes back as Talmar features or when the lock in section 5 is revisited. Names they change through Talmar.
- **Delivery:** PRs that each leave the build green: (1) this ADR, Docker Compose and the realm, (2) removal of the
  old registration code, (3) the identity-provider port and Keycloak adapter, (4a) the onboarding use case and its
  persistence, (4b) the contract, the resource server and the endpoint, (5) changing names (`PATCH /me`). Step 4 was
  split to stay within the PR size limit.

## Revisions

### 2026-10-18 — users change their names through Talmar

The maintainer asked for name editing right after onboarding. It is the "Talmar feature that writes through to
Keycloak" this ADR anticipated in section 5:

- `PATCH /me` with `firstName` and/or `lastName` (the contract's whitelist, the same as the realm's user profile).
  An omitted name stays as it is. The account must have onboarded (`404` otherwise): the copy in `app_user` is what
  Talmar shows, and there is no copy to change before onboarding.
- **Keycloak first, then the copy.** Keycloak is the source for login and its mails. When the Keycloak call fails,
  nothing has changed (`503`). When saving the copy fails after it, the two differ until the user repeats the
  request; both names always go to Keycloak, so a repeat brings them back in line. No distributed transaction.
- **Partial update.** The adapter sends `PUT /users/{id}` with only the two names. Keycloak 26.8 leaves every field
  that is missing from the representation as it is (checked against a running Keycloak, see below). Re-check this on
  a Keycloak upgrade; the fallback is to `GET` the user and `PUT` it back whole.
- The lock in section 5 stays: users still cannot edit their names in Keycloak itself, so Talmar is the only way in.
- **Rights:** no new role. `manage-users` already allows the update.

**Alternative considered — reopen the account console for names only.** One fewer endpoint, but the console is
all-or-nothing (it also exposes sessions, credentials and the e-mail field), and a change there does not reach the
copy without an event listener extension in Keycloak. Rejected for the same reason the lock exists.

## Verified in PR 1

Checked on 2026-10-18 against the Docker Compose setup (Keycloak 26.8.1, SQL Server 2025), by HTTP requests against
the registration form, the login flow, the account API and the Admin API:

- The registration form rejects `gmail.com`, `hotmail.nl`, `Outlook.com`, `live.co.uk`, `yandex.ru` and
  `t-online.de` with the custom message. The Admin API rejects public addresses with `400` on `email`. Work
  addresses pass.
- Names with a digit are rejected; `Łukasz Żółć-李` is accepted and stored intact (UTF-8 collation).
- The password is asked only after e-mail verification; shorter than 12 characters is rejected. The stored hash is
  Argon2id with 64 MiB, 3 iterations and parallelism 4, so the `KC_SPI_PASSWORD_HASHING__ARGON2__*` variables take
  effect.
- The account API refuses profile changes (`401`), and `kc_action=UPDATE_PROFILE` shows no profile form. A change
  of the e-mail address is ignored.
- The `talmar-backend` service account creates an organization (`201`, `409` on a repeat), finds it by exact domain,
  adds a member (`201`, `409` on a repeat) as `UNMANAGED`, and cannot read clients (`403`).
- With an organization in the realm, login is identity-first.

## Verified in PR 4

Checked on 2026-10-18 against the Docker Compose setup with the backend running on the `local` profile: a user logged
in through `talmar-web` (authorization code + PKCE), and `POST /onboarding` with that access token answered `200
ACTIVE`. Keycloak then held the organization (name and domain `smoke-test.nl`, alias the company id) with the user as
`UNMANAGED` member. A repeat answered `409`. This found one bug the H2 tests cannot: SQL Server's default `varchar`
collation stored `Łukasz` as `Lukasz`, so `app_user`'s name columns are now `nvarchar`.

## Verified in PR 5

Checked on 2026-10-18 against the Docker Compose setup: before onboarding, `PATCH /me` answered `404`; after it,
changing the last name and then the first name answered `204` twice. Keycloak then held `José` / `Żółć-李` with the
e-mail address, its verified flag and the required actions unchanged, and `app_user` held the same names. A name with
a digit answered `400`.
