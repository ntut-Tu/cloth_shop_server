# Return case adjudication

The return case API is available at `/api/return-cases`. It uses the existing cookie JWT and requires an `Idempotency-Key` header (8–128 safe characters) for every mutation. A customer may open a case for an item in their own paid order after delivery or completion. Each request specifies an `orderItemId`, a positive `quantity`, and a `reason`:

```json
{ "orderItemId": 120, "quantity": 1, "reason": "Size does not fit" }
```

The database locks the purchased order item while checking all live and approved case quantities. The total cannot exceed the purchased quantity. An item with any legacy `refund_request` cannot enter this workflow. The legacy create endpoint also checks for active return cases, so the two workflows cannot claim the same item concurrently.

The case starts in `VENDOR_PENDING` with a seven-day vendor response window. The assigned vendor can approve or reject using `POST /{caseId}/vendor-decision` with `{ "approved": true|false, "response": "..." }`. Approval closes the case and writes one immutable settlement decision. Rejection opens a seven-day customer appeal window. The owning customer can escalate with `POST /{caseId}/appeal` and `{ "reason": "..." }`; an admin then issues a final decision through `POST /{caseId}/admin-decision` with the same decision body. A customer can withdraw before escalation or approval through `POST /{caseId}/withdraw`.

An expiry worker uses `FOR UPDATE SKIP LOCKED` so multiple application instances can process deadlines without duplicate transitions. It escalates unanswered vendor cases to `ADMIN_PENDING` and closes unappealed vendor rejections as `EXPIRED`. Every transition is recorded in append-only `return_case_action`; database triggers prohibit updates and deletions. Repeating a request with the same idempotency key and payload returns the current case. Reusing a key with changed input is rejected.

`GET /api/return-cases?page=1&size=20` lists cases visible to the signed-in customer, assigned vendor, or admin. `GET /api/return-cases/{caseId}` returns the case, transition history, and settlement decision if approved. Customers and vendors cannot inspect cases they do not own.

Approved amount is allocated at decision time from the requested item's gross amount using the store order's net-to-subtotal ratio. The calculation uses the cumulative quantity already approved for the item, so splitting one return into several cases cannot lose rounding remainders. Shipping is excluded. The immutable settlement decision contains the case ID, quantity, amount, currency, source, and deciding user for a downstream refund processor. This workflow records authorization; it does not issue a payment refund or post legacy refund ledger entries.
