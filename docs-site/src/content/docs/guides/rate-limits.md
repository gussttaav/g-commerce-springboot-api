---
title: Rate limits
description: How many requests each client can send, the rate limit headers, and how to handle 429 Too Many Requests.
---

Every client has a bucket of requests. Each request takes one from the bucket, and the bucket refills gradually.
When it's empty, the API answers `429 Too Many Requests` until a request is available again.

## Limits

| Requests | Counted per | Burst | Refill per minute |
|---|---|---|---|
| Anonymous | IP address | 10 | 5 |
| Signed-in users | user | 30 | 20 |
| Administrators | user | 100 | 50 |
| Login attempts (`POST /api/auth/login`) | IP address | 5 | 1 |

**Burst** is the size of the bucket: how many requests you can send at once after a pause. **Refill** is how many
requests it gets back each minute, spread evenly: with a refill of 20, a request comes back every 3 seconds.

Login attempts have their own, stricter bucket, which makes guessing passwords impractical.
The limits apply to the `/api` endpoints; the discovery documents under `/.well-known` aren't limited.

## Headers

| Header | Sent with | Description |
|---|---|---|
| `X-Rate-Limit-Remaining` | Successful responses | Requests left in your bucket |
| `Retry-After` | `429` responses | Seconds to wait before the next request is accepted |
| `X-Rate-Limit-Retry-After-Seconds` | `429` responses | Same as `Retry-After` |

Browsers can read these headers from cross-origin responses too.

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 12
Content-Type: application/json

{
  "timestamp": "2026-10-20T09:30:00",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded",
  "path": "/api/products",
  "details": []
}
```

## Handling 429

Wait the seconds of `Retry-After`, then retry:

```js
async function fetchWithRetry(url, options = {}, attempts = 3) {
  for (let attempt = 1; ; attempt++) {
    const response = await fetch(url, options);
    if (response.status !== 429 || attempt === attempts) return response;
    const seconds = Number(response.headers.get('Retry-After') ?? 1);
    await new Promise((resolve) => setTimeout(resolve, seconds * 1000));
  }
}
```

To stay under the limits, watch `X-Rate-Limit-Remaining` and slow down before it reaches `0`, reuse data you already
have, and request pages of up to 100 items instead of many small pages.
