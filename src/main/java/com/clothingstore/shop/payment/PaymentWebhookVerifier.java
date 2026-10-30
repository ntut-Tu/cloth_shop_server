package com.clothingstore.shop.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class PaymentWebhookVerifier {
    private final byte[] secret;
    private final Clock clock;

    @Autowired
    public PaymentWebhookVerifier(@Value("${payment.webhook-secret:}") String secret) {
        this(secret, Clock.systemUTC());
    }

    PaymentWebhookVerifier(String secret, Clock clock) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public void verify(String timestamp, String signature, String rawBody) {
        if (secret.length < 32) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment provider callbacks are not configured");
        }
        if (timestamp == null || signature == null || rawBody == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing payment signature");
        }
        try {
            Instant sentAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
            if (Duration.between(sentAt, clock.instant()).abs().compareTo(Duration.ofMinutes(5)) > 0) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Expired payment signature");
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] expected = mac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8));
            byte[] received = HexFormat.of().parseHex(signature);
            if (!MessageDigest.isEqual(expected, received)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment signature");
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment signature");
        }
    }
}
