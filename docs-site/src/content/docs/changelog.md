---
title: Changelog
description: Changes to the G-Commerce API that affect its clients.
---

## October 2026

### Added

- This documentation site, with an interactive [API Reference](/api-reference/).
- The demo data is restored every day at 03:00 UTC, and the email, password and role of the
  [demo accounts](/introduction/#demo-accounts) can't be changed: those requests get `403 Forbidden`.
- Browsers can read the `Retry-After` and `X-Rate-Limit-*` headers of cross-origin responses, and the discovery
  documents under `/.well-known` accept cross-origin requests.

### Fixed

- `401` and `403` responses carry the CORS headers, so browsers can read them instead of reporting a CORS error.
- A `Bearer` header without a token counts as no token, and `POST /api/auth/login` ignores the `Authorization`
  header, so an empty or expired token no longer blocks public requests or signing in.

### Changed

- A `409 Conflict` for a taken email or product name now says which value is taken: "A user with this email already
  exists" or "A product with this name already exists".
- In the purchase history, `unitPrice` is the price paid, so it always matches `subtotal`. It used to be the product's
  current price.
- Tokens identify the user by ID in `sub`, so they keep working after an email change. Tokens issued before this
  change must be replaced by signing in again.
- Refresh tokens work once, logout revokes the access token immediately, and refresh tokens are rejected as access
  tokens.
- Anonymous requests are rate-limited per IP address, and login attempts have a stricter limit of their own.
- An unknown email and a wrong password get the same `401` at login.
- List endpoints validate `page`, `size` and `sort`: invalid values get `400` instead of `500`.
- Registering without a `role` creates a `USER`.
- Inactive products can't be bought.
