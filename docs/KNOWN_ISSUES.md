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

### Functional bugs

| ID | Severity | Issue |
|----|----------|-------|
| FUN-1 | Medium | **Registering without `role` fails in production.** `UserDTO.role` defaults to `null` when omitted (as in the README example). MySQL's `rol ... NOT NULL` rejects the insert (409 with a confusing message). In the H2 test schema the column is nullable, so the user is saved with a null role and gets a `ROLE_null` authority — which is why tests don't catch it. |
| FUN-2 | Medium | **Inactive products can be purchased.** `PurchaseService.createPurchase` looks products up by id without checking `active`. |
| FUN-3 | Medium | **Missing `productId`/`quantity` cause 500s.** `PurchaseProductDTO` uses `@Positive` without `@NotNull`, and `@Positive` accepts `null`; the service then fails with an NPE / `InvalidDataAccessApiUsageException`. |
| FUN-4 | Medium | **Unvalidated paging parameters.** `sort` is passed straight to `Sort.by`: an unknown field results in a 500, and any entity field is sortable (including `password` on `/api/users`). `size` has no upper bound. |
| FUN-5 | Low | **Changing email breaks the current session.** The JWT `sub` is the email, so after `PATCH /api/users/me` changes it, the user's existing tokens resolve to a non-existent user (401) with no explanation. |

### Configuration and polish

| ID | Severity | Issue |
|----|----------|-------|
| OPS-1 | Low | **`SslProfileDetector` overrides `SPRING_PROFILES_ACTIVE`** (which the README tells users to set), and its classpath check uses `ClassPathResource.getFile()`, which throws inside a packaged jar. |
| OPS-2 | Low | **Mixed-language API output.** Several error messages are in Spanish ("Ha ocurrido un error inesperado", "Error de validación", "Usuario no encontrado"), plus leftover Spanish identifiers (`toProductoResponseDTO`, test helpers) and `spring.application.name: gestion-tienda`. |
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

### Phase 4 — Functional correctness (FUN-1 … FUN-5)
1. Default `role` to `USER` in `UserDTO`'s compact constructor; add `@Column(nullable = false)` so H2 matches MySQL (FUN-1).
2. Reject inactive products in `createPurchase` (FUN-2).
3. Add `@NotNull` to `productId` and `quantity` (FUN-3).
4. Whitelist sort fields per resource, cap `size` (e.g. `@Max(100)`), and map invalid sort fields to 400 (FUN-4).
5. Use the user id as JWT `sub` (email as a claim), or return a new token pair from `PATCH /api/users/me` when the email changes (FUN-5).

### Phase 5 — Hardening and polish (KEY-3, OPS-1 … OPS-5)
1. Encrypt private keys with AES-GCM using a key derived from an environment secret (e.g. `OAUTH2_KEY_ENCRYPTION_SECRET`), never stored in Redis (KEY-3).
2. Only force the `https` profile when no profile is set explicitly; use `Resource.exists()` (OPS-1).
3. Translate remaining Spanish messages/identifiers; rename the application (OPS-2).
4. Add a `pull_request` trigger for the quality and test jobs (OPS-4).
5. `git update-index --chmod=+x mvnw` and drop the `chmod` from the Dockerfile (OPS-5).
5. Refresh the README (OPS-3) last, so it describes the fixed behavior.

### Beyond bug fixes (portfolio improvements to consider)
- **Flyway migrations** instead of `mysql-init/shopping_db.sql`, so schema and entities can't drift apart silently.
- **Testcontainers (MySQL + Redis)** for integration tests — this would have caught FUN-1, which H2 hides.
- **JaCoCo coverage report** in CI with a badge in the README.
