# Vendor order webhooks

Set `WEBHOOK_ALLOWED_HOSTS` to a comma-separated list of exact DNS hostnames owned or
approved by the operator. The empty default denies subscription creation. Wildcards,
IP literals, non-HTTPS URLs, redirects, query strings, fragments, and ports other than
443 are not accepted.

The dispatcher revalidates each endpoint before delivery. Its HTTP connection manager
also resolves the hostname and rejects the connection unless every address returned
at that connection attempt is public. Redirects and built-in HTTP retries are disabled;
the database delivery state controls retries.

The `X-Webhook-Signature` header contains `v1=` followed by lowercase hexadecimal
HMAC-SHA256 over the UTF-8 bytes of
`<X-Webhook-Timestamp>.<X-Webhook-Event-Id>.<exact JSON body>`. The secret is a
Base64url-encoded 32-byte key returned when the subscription is created or rotated.
`X-Webhook-Idempotency-Key` is the stable delivery ID across retries.

Each failed attempt is retried with bounded exponential backoff until the configured
limit. Exhausted pending deliveries, including those made exhausted by lowering
`webhooks.max-attempts`, move to `DEAD`. Disabling or deleting a subscription cancels
queued and leased deliveries. A request already in flight when cancellation occurs may
still reach the destination; receivers should use the idempotency key to deduplicate.
