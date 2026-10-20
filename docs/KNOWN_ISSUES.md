# Known Issues and Fix Plan

G-Commerce started as an educational project and is being upgraded into a portfolio piece.
This document lists the defects found during a code review (October 2026) and the plan to fix them.
Each issue has an ID so commits and PRs can reference it (e.g. `fix(auth): Revoke access tokens on logout (SEC-1)`).

Severity: **High** = security hole or production outage · **Medium** = wrong behavior / 500s · **Low** = polish.

## Issues

All the issues found in the review are fixed. The fix plan below summarizes how, and the commits that fixed each issue reference its ID.

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

### Phase 5 — Hardening and polish (KEY-3, OPS-1 … OPS-5) — done
RSA private keys are encrypted with AES-256-GCM under a key derived with PBKDF2 from `OAUTH2_KEY_ENCRYPTION_SECRET`, which is required (at least 32 characters) and never stored; each encryption uses a random IV and binds the key ID as associated data. A key stored by an earlier version cannot be decrypted, so it is rotated out on startup: it stops signing, its public key stays published until the tokens it signed expire, and its AES key is deleted from Redis. `SslProfileDetector` runs before the configuration files are loaded and only sets the default profile, `https` when a keystore exists at `/certs/keystore.p12` or on the classpath (checked with `Resource.exists()`) and `http` otherwise, so a profile set explicitly always wins; the keystore found becomes the default key store of the `https` profile (`SSL_KEY_STORE`). Error and validation messages, identifiers and test names are in English, and the application is named `g-commerce-api`; the database keeps its Spanish table and column names. Pull requests run the quality checks and the tests, while images are only built and deployed from pushes to `main`. `mvnw` is executable, and CI and the Dockerfile use it as is. The README describes the current behavior.

### Found while documenting the API (FUN-6) — done
A registration or profile update with an email that another user has, and a product created or renamed with the name of another product, got a 409 whose message came from the database error: on MySQL it named the table and column (`Already exists an entry with value 'usuario.email'`), and on H2, or when the conflict surfaced when the transaction committed, it was a generic "Data integrity violation". The services now check the email or name before saving and answer 409 "A user with this email already exists" or "A product with this name already exists"; keeping one's own email or a product's own name is not a conflict. The unique constraints still guard concurrent requests.

### Beyond bug fixes (portfolio improvements to consider)
- **Flyway migrations** instead of `mysql-init/shopping_db.sql`, so schema and entities can't drift apart silently.
- **Testcontainers (MySQL + Redis)** for integration tests — H2 hid FUN-1, and still differs from MySQL in other ways (e.g. column lengths).
- **JaCoCo coverage report** in CI with a badge in the README.
