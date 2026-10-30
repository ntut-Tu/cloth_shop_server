package com.clothingstore.shop.payment;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.service.JwtService;
import com.clothingstore.shop.utils.TokenUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/payment-intents")
public class PaymentController {
    private final PaymentService service;
    private final JwtService jwtService;

    public PaymentController(PaymentService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    public record CreateIntentRequest(Integer orderId) {}

    @PostMapping
    public ResponseEntity<ApiResponseDTO<PaymentView>> create(
            HttpServletRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateIntentRequest body) {
        if (body == null || body.orderId() == null || body.orderId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid orderId is required");
        }
        PaymentView intent = service.createIntent(customerId(request), body.orderId(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDTO<>(true, "Payment intent created", intent));
    }

    @GetMapping("/{intentId}")
    public ResponseEntity<ApiResponseDTO<PaymentView>> get(HttpServletRequest request, @PathVariable UUID intentId) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Payment intent retrieved",
                service.getIntent(customerId(request), intentId)));
    }

    @PostMapping("/{intentId}/capture")
    public ResponseEntity<ApiResponseDTO<PaymentView>> capture(
            HttpServletRequest request,
            @PathVariable UUID intentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Capture request recorded",
                service.requestCapture(customerId(request), intentId, idempotencyKey)));
    }

    @PostMapping("/{intentId}/cancel")
    public ResponseEntity<ApiResponseDTO<PaymentView>> cancel(HttpServletRequest request, @PathVariable UUID intentId) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Payment intent cancelled",
                service.cancelIntent(customerId(request), intentId)));
    }

    @PostMapping("/provider-events")
    public ResponseEntity<ApiResponseDTO<PaymentView>> providerEvent(
            @RequestHeader(value = "X-Payment-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Payment-Signature", required = false) String signature,
            @RequestBody String rawBody) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Provider event accepted",
                service.receiveProviderEvent(timestamp, signature, rawBody)));
    }

    private int customerId(HttpServletRequest request) {
        String token = TokenUtils.extractTokenFromCookies(request);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer authentication required");
        }
        try {
            if (!"customer".equals(jwtService.extractRole(token))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer role required");
            }
            Integer userId = jwtService.extractUserId(token);
            if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer authentication required");
            return userId;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid customer token");
        }
    }
}
