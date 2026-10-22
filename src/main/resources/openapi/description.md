The G-Commerce API is the backend of a demo online store: user accounts, a product catalog and purchases.
It is a Spring Boot application that issues and verifies its own RS256-signed JSON Web Tokens.

## Authentication

Most endpoints need an access token in the `Authorization` header:

```http
Authorization: Bearer <access token>
```

1. Get an access token and a refresh token with `POST /api/auth/login`.
2. The access token expires after {{accessTokenMinutes}} minutes. Exchange the refresh token for a new pair with
   `POST /api/auth/refresh`; each refresh token works once and expires after {{refreshTokenHours}} hours.
3. `POST /api/auth/logout` revokes the access token and the refresh token of the session.

Tokens are signed with rotating RSA keys, published at `/.well-known/jwks.json` for other services to verify them.

## Demo accounts

This deployment is a public demo, so anyone can sign in with these accounts and change the data:

| Email | Password | Role |
|---|---|---|
| `john@example.com` | `JohnDoe123!` | `USER`: can buy products |
| `admin@example.com` | `Admin123!` | `ADMIN`: can manage products and users |

The email, password and role of these accounts can't be changed, so they keep working for everyone; register your
own account to try those changes. The demo data is restored every day at 03:00 UTC, which deletes the accounts,
products and purchases that visitors created.

## Errors

Errors use conventional HTTP status codes and share one JSON body, the `Error` schema:

```json
{
  "timestamp": "2026-10-20T09:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found",
  "path": "/api/products/42",
  "details": []
}
```

`message` explains the error, and `details` lists the specific problems, such as one entry per invalid field.

## Pagination

List endpoints take `page` (0-based), `size` (1 to {{maxPageSize}}), `sort` and `direction` (`ASC` or `DESC`)
query parameters, and return the items in `content` with the page metadata.

## Rate limits

Every client gets a bucket of requests that refills over time:

| Requests | Counted per | Burst | Refill per {{rateLimitWindow}} |
|---|---|---|---|
| Anonymous | IP address | {{anonymousCapacity}} | {{anonymousRefill}} |
| Signed-in users | user | {{userCapacity}} | {{userRefill}} |
| Administrators | user | {{adminCapacity}} | {{adminRefill}} |
| Login attempts | IP address | {{loginCapacity}} | {{loginRefill}} |

Successful responses carry an `X-Rate-Limit-Remaining` header. When the bucket is empty, the API answers
`429 Too Many Requests` with a `Retry-After` header: the seconds to wait before trying again.
