package com.clothingstore.shop.webhook;

import com.clothingstore.shop.service.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WebhookService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JwtService jwtService;
    private final WebhookRepository repository;
    private final WebhookProperties properties;
    private final PublicWebhookDnsResolver dnsResolver;

    public WebhookService(JwtService jwtService, WebhookRepository repository, WebhookProperties properties,
                          PublicWebhookDnsResolver dnsResolver) {
        this.jwtService = jwtService;
        this.repository = repository;
        this.properties = properties;
        this.dnsResolver = dnsResolver;
    }

    public List<SubscriptionView> subscriptions(String token) {
        return repository.subscriptions(vendorId(token));
    }

    public List<DeliveryView> deliveries(String token, long subscriptionId, int limit) {
        int vendorId = vendorId(token);
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be between 1 and 100");
        }
        requireSubscription(vendorId, subscriptionId);
        return repository.deliveries(vendorId, subscriptionId, limit);
    }

    @Transactional
    public CreatedSubscription create(String token, String endpointUrl, Set<String> events) {
        int vendorId = vendorId(token);
        String url = validateEndpoint(endpointUrl);
        Set<String> eventTypes = validateEvents(events);
        String secret = newSecret();
        long id = repository.create(vendorId, url, secret, eventTypes);
        return new CreatedSubscription(repository.subscription(vendorId, id), secret);
    }

    @Transactional
    public SubscriptionView update(String token, long subscriptionId, String endpointUrl,
                                   boolean active, Set<String> events) {
        int vendorId = vendorId(token);
        requireSubscription(vendorId, subscriptionId);
        String url = validateEndpoint(endpointUrl);
        Set<String> eventTypes = validateEvents(events);
        if (repository.update(vendorId, subscriptionId, url, active, eventTypes) != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found");
        }
        return repository.subscription(vendorId, subscriptionId);
    }

    @Transactional
    public RotatedSecret rotateSecret(String token, long subscriptionId) {
        int vendorId = vendorId(token);
        String secret = newSecret();
        if (repository.rotateSecret(vendorId, subscriptionId, secret) != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found");
        }
        return new RotatedSecret(subscriptionId, secret);
    }

    @Transactional
    public void delete(String token, long subscriptionId) {
        int vendorId = vendorId(token);
        if (repository.delete(vendorId, subscriptionId) != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found");
        }
    }

    public String validateEndpoint(String endpointUrl) {
        final URI uri;
        try {
            uri = URI.create(endpointUrl);
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook URL", error);
        }
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null ||
                uri.getUserInfo() != null || uri.getFragment() != null || uri.getQuery() != null ||
                (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A public HTTPS endpoint on port 443 is required");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (!normalizedHost.contains(".") ||
                !normalizedHost.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?") ||
                normalizedHost.equals("localhost") || normalizedHost.endsWith(".localhost") ||
                normalizedHost.matches("[0-9.]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A DNS hostname is required");
        }
        boolean allowed = properties.getAllowedHosts() != null && properties.getAllowedHosts().stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(normalizedHost::equals);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook host is not allowlisted");
        }
        try {
            dnsResolver.resolve(normalizedHost);
        } catch (UnknownHostException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Webhook host must resolve only to public addresses", error);
        }
        return uri.normalize().toASCIIString();
    }

    private int vendorId(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        final Integer userId;
        final String role;
        try {
            userId = jwtService.extractUserId(token);
            role = jwtService.extractRole(token);
        } catch (JwtException | IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token", error);
        }
        if (userId == null || !"vendor".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active vendor account required");
        }
        Integer vendorId = repository.activeVendorId(userId);
        if (vendorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active vendor account required");
        }
        return vendorId;
    }

    private void requireSubscription(int vendorId, long subscriptionId) {
        if (subscriptionId <= 0 || repository.subscription(vendorId, subscriptionId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found");
        }
    }

    private Set<String> validateEvents(Set<String> events) {
        if (events == null || events.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one event type is required");
        }
        Set<String> allowed = Arrays.stream(WebhookEventType.values()).map(Enum::name).collect(Collectors.toSet());
        Set<String> validated = new LinkedHashSet<>();
        for (String event : events) {
            if (event == null || !allowed.contains(event)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown webhook event: " + event);
            }
            validated.add(event);
        }
        return validated;
    }

    private String newSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record CreatedSubscription(SubscriptionView subscription, String secret) { }
    public record RotatedSecret(long subscriptionId, String secret) { }
}
