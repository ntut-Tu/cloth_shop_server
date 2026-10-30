# Store-order transition API

Vendor fulfillment updates use a versioned state machine. The current graph allows `Pending` to become `Shipped` or `Cancelled`; terminal states have no outgoing transition. Only an active vendor who owns the store order can read or change it.

## Discover state

`GET /api/orders/storeOrder/{storeOrderId}/transition-state` returns the current `status`, `version`, and `allowedTargets`. Read this before submitting a transition. Authentication uses the existing `token` cookie.

## Submit a transition

`POST /api/orders/storeOrder/{storeOrderId}/transitions` accepts JSON:

```json
{
  "targetStatus": "Shipped",
  "expectedVersion": 0,
  "idempotencyKey": "shipment-41524",
  "reason": "Parcel handed to carrier"
}
```

`expectedVersion` is the version returned by state discovery. A successful transition returns its `transitionId`, actor, prior and new status, resulting version, idempotency key, reason, and timestamp. Repeating the same request with the same key returns the original result with `replayed=true`; reusing a key for a different command returns HTTP 409. Concurrent updates with a stale version also return HTTP 409.

Each store order has its own idempotency key namespace. Keys may contain letters, digits, `.`, `_`, `:`, and `-`, up to 100 characters. Reasons are optional and limited to 500 characters.

## Read history

`GET /api/orders/storeOrder/{storeOrderId}/transitions?limit=20` returns newest transitions first. To load older entries, pass the last received `transitionId` as `beforeTransitionId`. The limit must be between 1 and 100.

## Legacy status route

`POST /api/orders/storeOrder/{storeOrderId}/status` remains available and now uses the same ownership and state-transition policy. It creates an audit entry and version increment for a successful change. New clients should use the versioned transition endpoint for safe retries and concurrency handling.

Missing or invalid credentials return HTTP 401. Inactive vendors and cross-vendor access return HTTP 403. Unknown orders return HTTP 404. Invalid input returns HTTP 400; invalid transitions, stale versions, and conflicting idempotency keys return HTTP 409.
