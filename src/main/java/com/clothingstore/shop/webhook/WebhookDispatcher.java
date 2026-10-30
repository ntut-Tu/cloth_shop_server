package com.clothingstore.shop.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class WebhookDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(WebhookDispatcher.class);
    private final WebhookRepository repository;
    private final WebhookService service;
    private final WebhookProperties properties;
    private final CloseableHttpClient client;

    public WebhookDispatcher(WebhookRepository repository, WebhookService service,
                             WebhookProperties properties, CloseableHttpClient webhookHttpClient) {
        this.repository = repository;
        this.service = service;
        this.properties = properties;
        this.client = webhookHttpClient;
    }

    @Scheduled(fixedDelayString = "${webhooks.dispatch-interval-ms:5000}")
    public void dispatchDue() {
        repository.expireExhausted(properties.getMaxAttempts());
        // Claim just before sending so no delivery waits in memory past its lease.
        for (int i = 0; i < properties.getBatchSize(); i++) {
            var batch = repository.claim(1, properties.getMaxAttempts(), properties.getLeaseSeconds());
            if (batch.isEmpty()) return;
            if (!dispatch(batch.get(0))) return;
        }
    }

    private boolean dispatch(DeliveryAttempt attempt) {
        Integer responseStatus = null;
        try {
            URI endpoint = URI.create(service.validateEndpoint(attempt.endpointUrl()));
            long timestamp = System.currentTimeMillis() / 1_000;
            String signature = signature(attempt, timestamp);
            HttpPost request = new HttpPost(endpoint);
            request.setEntity(new StringEntity(attempt.payload(),
                    ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
            request.setHeader("X-Webhook-Event", attempt.eventType());
            request.setHeader("X-Webhook-Event-Id", Long.toString(attempt.eventId()));
            request.setHeader("X-Webhook-Delivery", Long.toString(attempt.id()));
            request.setHeader("X-Webhook-Idempotency-Key", Long.toString(attempt.id()));
            request.setHeader("X-Webhook-Timestamp", Long.toString(timestamp));
            request.setHeader("X-Webhook-Signature", "v1=" + signature);
            // The client's DNS resolver validates the same addresses it hands to the socket connector.
            try (var response = client.executeOpen(HttpHost.create(endpoint), request, HttpClientContext.create())) {
                // Only the status is needed; close the socket without draining an unbounded body.
                responseStatus = response.getCode();
            }
            if (responseStatus >= 200 && responseStatus < 300) {
                repository.delivered(attempt, responseStatus);
                return true;
            }
            recordFailure(attempt, responseStatus, "HTTP " + responseStatus);
            return true;
        } catch (IOException | RuntimeException error) {
            recordFailure(attempt, responseStatus, error.getClass().getSimpleName());
            return !Thread.currentThread().isInterrupted();
        }
    }

    private void recordFailure(DeliveryAttempt attempt, Integer responseStatus, String reason) {
        boolean exhausted = attempt.attempts() >= properties.getMaxAttempts();
        long delay = retryDelay(attempt.attempts());
        OffsetDateTime nextAttempt = OffsetDateTime.now(ZoneOffset.UTC).plusNanos(delay * 1_000_000);
        repository.failed(attempt, responseStatus, reason, nextAttempt, exhausted);
        if (exhausted) LOG.warn("Webhook delivery {} reached the retry limit", attempt.id());
    }

    private long retryDelay(int attempts) {
        long delay = properties.getBaseDelayMs();
        for (int i = 1; i < attempts; i++) {
            delay = Math.min(properties.getMaxDelayMs(), delay > Long.MAX_VALUE / 2 ? Long.MAX_VALUE : delay * 2);
        }
        return delay;
    }

    private String signature(DeliveryAttempt attempt, long timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getUrlDecoder().decode(attempt.secret()), "HmacSHA256"));
            String message = timestamp + "." + attempt.eventId() + "." + attempt.payload();
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("Webhook signing failed", error);
        }
    }
}
