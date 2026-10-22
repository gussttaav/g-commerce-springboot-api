---
title: Errors
description: The JSON body of every error response, and what each HTTP status code means in the API.
---

The API answers errors with a conventional HTTP status code and the same JSON body:

```json
{
  "timestamp": "2026-10-20T09:30:00",
  "status": 400,
  "error": "Validation Error",
  "message": "Validation failed",
  "path": "/api/users",
  "details": [
    "email: Email must be valid",
    "password: Password is required"
  ]
}
```

| Field | Description |
|---|---|
| `timestamp` | When the error occurred |
| `status` | The HTTP status code, repeated for convenience |
| `error` | A short name for the kind of error |
| `message` | What went wrong, meant for developers |
| `path` | The path of the request |
| `details` | The specific problems, such as one entry per invalid field; it may be empty |

Branch on `status`, and on `error` when one status has several causes. `message` and `details` are written for
people, and their wording may change.

## Status codes

| Status | `error` | When |
|---|---|---|
| `400 Bad Request` | `Validation Error` | A body field is invalid; `details` names each field and the rule it broke |
| | `Constraint Violation` | A query parameter is invalid, such as `size=500` |
| | `Type Mismatch` | A parameter has the wrong type, such as `page=first` |
| | `JSON Parse Error` | The body isn't valid JSON |
| | `Bad Request` | A rule of the operation failed, such as a wrong current password when changing it |
| `401 Unauthorized` | `Unauthorized` | The access token is missing, invalid, expired or revoked, or the login credentials are wrong |
| | `Invalid Token` | The token sent to `/api/auth/refresh` or `/api/auth/validate` can't be used |
| | `Missing Authorization Header` | `/api/auth/refresh` or `/api/auth/validate` got no `Authorization` header |
| `403 Forbidden` | `Forbidden` | Your role can't use the endpoint, such as a user creating a product |
| | `Access Denied` | The operation needs more privileges, such as registering an administrator |
| `404 Not Found` | `Not Found` | The resource doesn't exist, or a product to buy is inactive |
| `409 Conflict` | `Conflict` | A unique value is taken: a user's email or a product's name |
| `429 Too Many Requests` | `Too Many Requests` | Your [rate limit](/guides/rate-limits/) is exhausted; wait the seconds of `Retry-After` |
| `500 Internal Server Error` | `Internal Server Error` | An unexpected error; retrying later may work |

The [API Reference](/api-reference/) lists the errors of each endpoint, with example bodies.

## Handling errors

```js
const response = await fetch('https://g-commerce.duckdns.org/api/products', {
  method: 'POST',
  headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
  body: JSON.stringify(product),
});

if (!response.ok) {
  const error = await response.json();
  if (error.status === 401) {
    // Refresh the access token, or sign in again, and retry
  } else if (error.status === 400) {
    console.error(error.message, error.details); // e.g. ["price: The price of the product must be greater than 0"]
  } else {
    throw new Error(`${error.status} ${error.error}: ${error.message}`);
  }
}
```
