package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.utils.TokenUtils;
import com.clothingstore.shop.webhook.DeliveryView;
import com.clothingstore.shop.webhook.SubscriptionView;
import com.clothingstore.shop.webhook.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/vendor/webhooks")
public class VendorWebhookController {
    private final WebhookService service;

    public VendorWebhookController(WebhookService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponseDTO<List<SubscriptionView>> list(HttpServletRequest request) {
        return new ApiResponseDTO<>(true, "Webhook subscriptions fetched",
                service.subscriptions(token(request)));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<WebhookService.CreatedSubscription>> create(
            HttpServletRequest request, @RequestBody CreateRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(true,
                "Webhook subscription created", service.create(token(request), body.endpointUrl(), body.events())));
    }

    @PutMapping("/{subscriptionId}")
    public ApiResponseDTO<SubscriptionView> update(HttpServletRequest request,
                                                     @PathVariable long subscriptionId,
                                                     @RequestBody UpdateRequest body) {
        return new ApiResponseDTO<>(true, "Webhook subscription updated",
                service.update(token(request), subscriptionId, body.endpointUrl(), body.active(), body.events()));
    }

    @PostMapping("/{subscriptionId}/rotate-secret")
    public ApiResponseDTO<WebhookService.RotatedSecret> rotateSecret(HttpServletRequest request,
                                                                      @PathVariable long subscriptionId) {
        return new ApiResponseDTO<>(true, "Webhook secret rotated",
                service.rotateSecret(token(request), subscriptionId));
    }

    @DeleteMapping("/{subscriptionId}")
    public ApiResponseDTO<Void> delete(HttpServletRequest request, @PathVariable long subscriptionId) {
        service.delete(token(request), subscriptionId);
        return new ApiResponseDTO<>(true, "Webhook subscription deleted", null);
    }

    @GetMapping("/{subscriptionId}/deliveries")
    public ApiResponseDTO<List<DeliveryView>> deliveries(HttpServletRequest request,
                                                           @PathVariable long subscriptionId,
                                                           @RequestParam(defaultValue = "50") int limit) {
        return new ApiResponseDTO<>(true, "Webhook deliveries fetched",
                service.deliveries(token(request), subscriptionId, limit));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode())
                .body(new ApiResponseDTO<>(false, error.getReason(), null));
    }

    private String token(HttpServletRequest request) {
        return TokenUtils.extractTokenFromCookies(request);
    }

    public record CreateRequest(String endpointUrl, Set<String> events) { }
    public record UpdateRequest(String endpointUrl, Set<String> events, boolean active) { }
}
