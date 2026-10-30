package com.clothingstore.shop.payment;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentWebhookVerifierTest {
    private static final String SECRET = "a-long-test-provider-secret-32-bytes";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private final PaymentWebhookVerifier verifier = new PaymentWebhookVerifier(SECRET, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void acceptsExactSignedPayloadWithinTimeWindow() throws Exception {
        String body = "{\"eventId\":\"one\"}";
        String timestamp = Long.toString(NOW.getEpochSecond());
        assertDoesNotThrow(() -> verifier.verify(timestamp, sign(timestamp, body), body));
    }

    @Test
    void rejectsModifiedPayloadAndExpiredTimestamp() throws Exception {
        String body = "{\"eventId\":\"one\"}";
        String timestamp = Long.toString(NOW.getEpochSecond());
        assertThrows(ResponseStatusException.class,
                () -> verifier.verify(timestamp, sign(timestamp, body), "{\"eventId\":\"two\"}"));

        String stale = Long.toString(NOW.minusSeconds(301).getEpochSecond());
        assertThrows(ResponseStatusException.class,
                () -> verifier.verify(stale, sign(stale, body), body));
    }

    @Test
    void rejectsCallbacksWhenSecretIsNotConfigured() {
        PaymentWebhookVerifier disabled = new PaymentWebhookVerifier("", Clock.fixed(NOW, ZoneOffset.UTC));
        assertThrows(ResponseStatusException.class,
                () -> disabled.verify(Long.toString(NOW.getEpochSecond()), "00", "{}"));
    }

    private String sign(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }
}
