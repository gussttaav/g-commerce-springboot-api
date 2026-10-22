---
title: Pagination
description: How list endpoints return their items one page at a time, and the parameters that control the page and the order.
---

The endpoints that list products, purchases and users return one page of items at a time.

## Parameters

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | The page to return, starting at `0` |
| `size` | `10` | Items per page, from `1` to `100` |
| `sort` | Depends on the list | The field to sort by; each list accepts its own fields |
| `direction` | Depends on the list | `ASC` or `DESC` |

| List | Sortable fields | Default order |
|---|---|---|
| `GET /api/products` | `name`, `price` | `name`, `ASC` |
| `GET /api/purchases` | `date`, `total` | `date`, `DESC` (newest first) |
| `GET /api/users` | `email`, `name`, `creationDate` | `email`, `ASC` |

An invalid value, such as `size=500` or `sort=password`, gets `400 Bad Request`, and `details` names the parameter:

```json
{
  "status": 400,
  "error": "Constraint Violation",
  "message": "Validation failed",
  "details": ["size: must be less than or equal to 100"]
}
```

## Response

The items are in `content`, next to the page metadata:

```json
{
  "content": [
    { "id": 2, "name": "Wireless Mouse", "price": 29.99, "...": "..." }
  ],
  "pageNumber": 1,
  "pageSize": 10,
  "totalElements": 24,
  "totalPages": 3,
  "isLastPage": false
}
```

| Field | Description |
|---|---|
| `content` | The items of the page; empty when the page is past the end |
| `pageNumber` | The page returned, starting at `0` |
| `pageSize` | The requested `size` |
| `totalElements` | The number of items in all the pages |
| `totalPages` | The number of pages |
| `isLastPage` | `true` on the last page |

## Reading every page

Request pages until `isLastPage` is `true`:

```js
async function fetchAllProducts() {
  const products = [];
  for (let page = 0; ; page++) {
    const response = await fetch(`https://g-commerce.duckdns.org/api/products?page=${page}&size=100`);
    const result = await response.json();
    products.push(...result.content);
    if (result.isLastPage) return products;
  }
}
```

Pages are computed on each request, so a product added or removed while you read can shift the following pages.
