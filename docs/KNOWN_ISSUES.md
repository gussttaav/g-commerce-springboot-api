# Known Issues and Fix Plan

G-Commerce started as an educational project and is being upgraded into a portfolio piece.
This document lists the defects found during a code review (October 2026) and the plan to fix them.
Each issue has an ID so commits and PRs can reference it (e.g. `fix(auth): Revoke access tokens on logout (SEC-1)`).

Severity: **High** = security hole or production outage · **Medium** = wrong behavior / 500s · **Low** = polish.

## Issues

### JWT key management

| ID | Severity | Issue |
|----|----------|-------|
| KEY-3 | Medium | **Private-key encryption gives no protection.** The AES key that encrypts each RSA private key is stored in the same Redis instance right next to the ciphertext (`oauth2:encryption:{kid}`), and the cipher uses ECB mode (`AES/ECB/PKCS5Padding`). |

### Configuration and polish

| ID | Severity | Issue |
|----|----------|-------|
| OPS-1 | Low | **`SslProfileDetector` overrides `SPRING_PROFILES_ACTIVE`** (which the README tells users to set), and its classpath check uses `ClassPathResource.getFile()`, which throws inside a packaged jar. |
| OPS-2 | Low | **Mixed-language API output.** Several error messages are in Spanish ("Ha ocurrido un error inesperado", "Error de validación"), plus leftover Spanish identifiers (`toProductoResponseDTO`, test helpers) and `spring.application.name: gestion-tienda`. |
| OPS-3 | Low | **README is out of date.** Wrong Spring Boot version, login response shows `usuario` (now `user`), single-test example uses `UsuarioControllerTest`, and the HTTPS instructions don't match OPS-1. |
| OPS-4 | Low | **CI only runs on pushes to `main`.** Pull requests get no Checkstyle/PMD/test feedback before merging. |
| OPS-5 | Low | **`mvnw` is not executable.** It is committed with mode `100644`, so `./mvnw` fails with "Permission denied" on Linux/macOS (the Dockerfile works around it with `chmod +x`). |

## Fix plan

Each phase is one branch and one PR. Every fix starts with a failing test that reproduces the issue (most of these slipped through because no test covered them), then the fix.

### Phase 1 — Key management (KEY-1, KEY-2, SEC-7) — done
The decoder looks keys up by `kid` in Redis, the active key never expires, a rotated-out key is kept for the longest token lifetime plus 1h, the JWKS publishes every key that is still kept, issuer and audience are validated, and scheduled rotation runs behind a Redis lock.

### Phase 2 — Token lifecycle (SEC-1, SEC-2, SEC-3, SEC-4) — done
The resource server decodes access tokens with validators that reject refresh tokens and tokens whose `jti` is blacklisted or missing; logout blacklists the access token's `jti` and deletes the refresh token. Refresh tokens are stored by `jti` and are single-use: the entry is deleted atomically before a new pair is issued, so a replayed token gets 401 even under concurrent requests. `/api/auth/refresh` and `/api/auth/validate` bypass the bearer token filter and decode the token themselves (refresh tokens with a decoder that only accepts refresh tokens), and `/api/auth/validate` reports an invalid token as `valid: false` with the reason.

### Phase 3 — Abuse protection (SEC-5, SEC-6) — done
Anonymous requests (`AnonymousAuthenticationToken` included) are limited per client IP address with the `unauthenticated` limits, and login attempts have a stricter bucket of their own per client IP address (`rate-limit.login`). Users get the limits of the role in their current token, since buckets are keyed by type and client. Buckets live in a size-bounded Caffeine cache that evicts a bucket once it has been unused long enough to refill, so eviction never resets a limit; they are per application instance (several instances would need the Bucket4j Redis backend). The client IP address is the request's remote address; behind a reverse proxy, `server.forward-headers-strategy=native` takes it from the `X-Forwarded-For` header of trusted proxies only. Login answers an unknown email and a wrong password with the same 401 "Invalid email or password", through Spring Security's `DaoAuthenticationProvider`, which also checks a dummy password hash for unknown emails so the response time doesn't reveal them. Registration still answers 409 for a registered email, now rate-limited per IP address; hiding that would need an email verification flow.

### Phase 4 — Functional correctness (FUN-1 … FUN-5) — done
A registration without a role gets the `USER` role, and the entities declare the `NOT NULL` columns of `mysql-init/shopping_db.sql`, so the H2 test schema rejects the same nulls as MySQL. Purchases only find active products: an inactive (removed) product is reported as not found, with its ID in the message. Purchase items require `productId` and `quantity`, and a null item is rejected. The listings validate their paging parameters: `sort` must be one of the fields the listing documents (products `name`, `price`; users `email`, `name`, `creationDate`; purchases `date`, `total`), `page` must not be negative and `size` must be between 1 and 100 (`PaginatedResponse.MAX_PAGE_SIZE`); invalid values get a 400 whose details name the parameter. Tokens identify the user by ID in `sub`, with the email as an informative `email` claim, so a session survives an email change, and a refresh token can no longer issue tokens to whoever registers its user's old email. Tokens with an email as subject, issued before this change, are rejected with 401, so their users have to sign in again.

### Phase 5 — Hardening and polish (KEY-3, OPS-1 … OPS-5)
1. Encrypt private keys with AES-GCM using a key derived from an environment secret (e.g. `OAUTH2_KEY_ENCRYPTION_SECRET`), never stored in Redis (KEY-3).
2. Only force the `https` profile when no profile is set explicitly; use `Resource.exists()` (OPS-1).
3. Translate remaining Spanish messages/identifiers; rename the application (OPS-2).
4. Add a `pull_request` trigger for the quality and test jobs (OPS-4).
5. `git update-index --chmod=+x mvnw` and drop the `chmod` from the Dockerfile (OPS-5).
5. Refresh the README (OPS-3) last, so it describes the fixed behavior.

### Beyond bug fixes (portfolio improvements to consider)
- **Flyway migrations** instead of `mysql-init/shopping_db.sql`, so schema and entities can't drift apart silently.
- **Testcontainers (MySQL + Redis)** for integration tests — H2 hid FUN-1, and still differs from MySQL in other ways (e.g. column lengths).
- **JaCoCo coverage report** in CI with a badge in the README.
