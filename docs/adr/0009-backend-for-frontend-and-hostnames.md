# ADR 0009 — A backend-for-frontend keeps tokens out of the browser

- **Status:** Accepted; the hostname of the test environment is open (section 4)
- **Date:** 2026-11-21
- **Supersedes:** the public client `taldo-web` of ADR 0006, section 8, and the frontend's part of ADR 0006, section 2
  (the browser logs in with authorization code + PKCE and holds the tokens).

## Context

Today the frontend is a public OIDC client: `keycloak-js` logs in with authorization code + PKCE and keeps the access,
refresh and ID tokens in the browser's memory. That is the standard setup for a single-page app, and the frontend team
built it carefully (memory only, silent renewal). Its weak point is the browser itself: a script that runs in the
frontend (cross-site scripting, a compromised npm dependency) can read those tokens and use them elsewhere.

Two decisions made that weak point matter more:

- **Sessions of up to 36 hours** (ADR 0006, revision 2026-11-21, point 6). A stolen refresh token stays usable for up
  to 24 hours idle and 36 hours in total.
- **Corporate customers.** Recruiters handle CVs, and their employers will ask how tokens are protected.

The IETF OAuth working group's guidance for browser-based apps ("OAuth 2.0 for Browser-Based Applications") ranks a
backend-for-frontend (BFF) as the most secure pattern: the tokens never reach the browser.

Constraints: the maintainer wants the architecture not to be advertised in what users see; production hosting is not
decided; EU only (`.claude/CLAUDE.md`).

## Decision

### 1. A separate module `taldo-bff`

A third Maven module, a small Spring Boot application with no business logic and no data of its own:

- **Login** with Spring Security's `oauth2Login` as a **confidential** client: authorization code + PKCE with a client
  secret, so a stolen authorization code is useless twice over.
- **Session** on the server, in PostgreSQL through Spring Session JDBC (no Redis to host). The browser gets only an
  opaque session cookie.
- **Relay:** every API call from the browser goes to the BFF, which adds the user's access token and forwards it to
  taldo-api. It renews the access token with the refresh token when needed.
- **Logout** ends the BFF session and the Keycloak session (RP-initiated logout).
- **CSRF:** the session is a cookie, so every state-changing request needs a CSRF token (Spring Security's cookie-to-
  header pattern), also with `SameSite=Strict` (section 3).

taldo-api does not change: it stays a stateless resource server for bearer tokens (ADR 0006, revision 2026-11-21).
Machine clients that come later (the multiposting aggregator, partner APIs) call it with bearer tokens, not through
the BFF.

**Alternatives considered:**

- **Keep the public client in the browser.** No extra component, but the tokens stay readable to any script in the
  frontend for up to 36 hours. Rejected together with the session lifetimes.
- **The BFF inside taldo-api** (a second filter chain for the browser). Saves a container, but makes the API
  stateful, gives it two ways to authenticate (cookie and bearer) and puts CSRF protection on every endpoint. Rejected:
  the API stays a clean resource server.
- **DPoP** (tokens bound to a key in the browser). Keycloak supports it and it stops replay of a stolen token, but the
  token is still in the browser and the frontend has to sign every request. The BFF removes the token from the browser
  altogether. Not chosen; reconsider for machine clients.

### 2. What the user sees is neutral

The BFF serves the same origin as the frontend. Nothing a user sees names it:

| Visible to the user | Spring default | Taldo |
|---------------------|----------------|-------|
| API calls | – | `/api/v1/**`, the same paths as taldo-api |
| Start of login | `/oauth2/authorization/{registration}` | `/login` |
| Return from Keycloak | `/login/oauth2/code/{registration}` | `/login/callback` |
| Logout | `/logout` | `/logout` |
| Session cookie | `SESSION` | `__Host-session` |
| `client_id` in the redirect to Keycloak | – | `taldo-web`: the confidential client takes over the name of the public one |
| Errors | whitelabel page | problem details without internal details |

This is tidiness, not security. Keycloak stays recognisable in the URLs of its login page, and a cookie session is a
common pattern; the setup has to be secure for an attacker who knows the architecture. Neutral names cost nothing, so
they are used.

The module is called `taldo-bff` in the code: users never see module names, and the code names things after what
they are.

### 3. Same origin and the strictest cookie

The frontend and the BFF share one origin (`app.taldo.eu` in production); a reverse proxy sends `/api/v1/**`,
`/login`, `/login/callback` and `/logout` to the BFF and everything else to the frontend's static files.

- No CORS: the browser only ever talks to its own origin.
- The session cookie is `__Host-session`: `Secure`, `HttpOnly`, `SameSite=Strict`, `Path=/`, no `Domain`. The
  `__Host-` prefix makes the browser refuse the cookie unless all of that holds, so it is never sent to another host,
  including other hosts under `taldo.eu`.
- Locally the Vite dev server forwards the same paths to the BFF, so the frontend uses relative paths everywhere.
  Whether browsers accept a `__Host-` cookie on `http://localhost` is checked when the BFF is built; if not, the cookie
  name is configurable per environment.

**Alternative considered — the BFF on its own subdomain** (e.g. `auth.taldo.eu`). Works, because subdomains of
`taldo.eu` are the same site, but needs CORS with credentials and gains nothing. Rejected; `auth.taldo.eu` goes to
Keycloak (section 4).

### 4. Hostnames

| | Local | Test | Production |
|---|---|---|---|
| Marketing site | – | – | `taldo.eu` |
| Frontend + BFF | `localhost:5173` | open | `app.taldo.eu` |
| Keycloak | `localhost:8080` | open | `auth.taldo.eu` |
| taldo-api | `localhost:8090` | private network | private network |

- **Keycloak on `auth.taldo.eu`:** users see it on the login page, it is the issuer in every token and, later, the
  address customers configure for SSO.
- **taldo-api has no public hostname.** Only the BFF calls it, so it can sit in a private network: nobody on the
  internet can reach the API directly. An integration that needs it from outside gets its own hostname when it is
  built.
- **One Keycloak per environment.** Test accounts never live in the production realm.
- **Test hostnames are open.** The options, from strongest to weakest:
  - A separate registrable domain (e.g. `taldo-test.eu`): a different site from production, so a compromised test
    environment cannot send same-site requests to production, and no certificate is valid for both. About €10 a year.
  - A subzone (`app.test.taldo.eu`): its own wildcard certificate, but the same site as production.
  - A prefix (`test-app.taldo.eu`): the same site as production, and either a certificate per host or a `*.taldo.eu`
    wildcard whose key would also be valid for production.

  Advised: a separate domain. Decided when the test environment is built.
- If the marketing site runs on an external CMS, it is the same site as the app. That is one reason the CSRF token
  stays next to `SameSite=Strict`.

### 5. Session lifetimes stay aligned

There are two sessions: Keycloak's (which bounds the refresh token) and the BFF's. The BFF's idle timeout is set from
the same value as Keycloak's SSO session idle (24 hours). When Keycloak refuses to renew a token because its session
ended, the BFF invalidates its own session and answers `401`; the frontend then sends the user to `/login`.

Refresh-token rotation (Keycloak's "revoke refresh token") is decided when the BFF is built. With rotation, two
parallel requests that both renew the token race, and the loser ends the session; with the refresh token on the
server only, rotation adds less than it does in a browser.

### 6. Keycloak clients

- `taldo-web` becomes confidential, with only the BFF's callback as redirect URI. It keeps the lightweight access
  token, the `sub` mapper and the `taldo-api` audience (ADR 0006, revision 2026-11-21).
- The public client disappears from every environment except local, where a `taldo-dev` client lets a developer get a
  token for Swagger UI or Postman. As long as a public client exists in an environment, anyone can obtain a token in a
  browser there, which is what the BFF exists to prevent.

## Consequences

- **Frontend:** `keycloak-js` goes. "Am I logged in?" becomes `GET /api/v1/me`; a `401` sends the browser to `/login`.
  The HTTP client sends the CSRF header instead of a bearer token, and the API base URL becomes the relative
  `/api/v1`. The name shown in the app comes from `GET /me`, because there is no ID token in the browser any more. The
  Keycloak login theme and the sign-up screens do not change.
- **taldo-api:** no code change. Its CORS configuration can go once no browser calls it directly.
- **Build:** a third module in the reactor, with the same static analysis as taldo-api; a guardrail change.
- **Running cost:** one more container (roughly 256–512 MB) and a session table in PostgreSQL.
- **Order:** built after #103. Until then the frontend keeps its public client, and the session lifetimes of ADR 0006
  are accepted for local development only.
