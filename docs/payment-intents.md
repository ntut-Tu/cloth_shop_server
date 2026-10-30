# Payment intent workflow

Card orders begin with `pay_status=PENDING`. A customer creates one intent for an owned card order with `POST /api/payment-intents` and an `Idempotency-Key` header. The request body is `{ "orderId": 123 }`. The response freezes the order amount in TWD. Repeating the same key returns the same intent; a different key cannot create a second intent for that order.

`POST /api/payment-intents/{intentId}/capture` also requires an `Idempotency-Key`. It records one durable capture attempt and returns its `attemptId`. A payment provider integration must use that attempt ID as its correlation key and submit signed results to `POST /api/payment-intents/provider-events`. This repository does not contain a card processor or accept card details; recording a capture request does not charge the customer or mark the order paid.

Set `PAYMENT_WEBHOOK_SECRET` to a secret of at least 32 UTF-8 bytes shared with the provider integration. The callback signs the exact UTF-8 request body with HMAC-SHA256 over `<X-Payment-Timestamp>.<raw-body>`, where the timestamp is Unix epoch seconds. Send the lowercase hexadecimal signature as `X-Payment-Signature`. Timestamps more than five minutes from server time are rejected. Every event must have a unique `eventId` and the following JSON fields:

```json
{
  "eventId": "provider-event-123",
  "attemptId": "00000000-0000-0000-0000-000000000001",
  "type": "CAPTURE_SUCCEEDED",
  "amount": 1000,
  "currency": "TWD",
  "providerReference": "provider-transaction-123"
}
```

Valid event types are `CAPTURE_SUCCEEDED`, `CAPTURE_FAILED`, and `SETTLED`. A failure may include `failureCode`. Successful capture changes the attempt to `SUCCEEDED`, intent to `CAPTURED`, and order to `PAID` in one database transaction. A later settlement event changes the intent to `SETTLED`. A failed capture allows a new attempt with a new idempotency key. Customer cancellation is allowed only before a capture begins or after a failed attempt. Replayed events with the same ID and body are safe; changed payloads and conflicting state transitions are rejected.

`GET /api/payment-intents/{intentId}` returns the current state to the owning customer. A gateway adapter can poll or receive the returned attempt ID from the checkout caller; production charging requires a separately configured provider adapter.
