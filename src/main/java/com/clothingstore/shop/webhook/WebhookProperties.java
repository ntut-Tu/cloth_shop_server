package com.clothingstore.shop.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "webhooks")
public class WebhookProperties {
    private List<String> allowedHosts = new ArrayList<>();
    private int batchSize = 25;
    private int maxAttempts = 8;
    private long baseDelayMs = 5_000;
    private long maxDelayMs = 3_600_000;
    private long requestTimeoutMs = 10_000;
    private long connectTimeoutMs = 3_000;
    private long leaseSeconds = 30;

    @PostConstruct
    public void validate() {
        if (batchSize < 1 || batchSize > 100 || maxAttempts < 1 || maxAttempts > 20 ||
                baseDelayMs < 1 || maxDelayMs < baseDelayMs || maxDelayMs > 86_400_000 ||
                connectTimeoutMs < 1 || requestTimeoutMs < connectTimeoutMs ||
                leaseSeconds * 1_000 <= requestTimeoutMs + 5_000) {
            throw new IllegalStateException("Invalid webhook dispatcher configuration");
        }
    }

    public List<String> getAllowedHosts() { return allowedHosts; }
    public void setAllowedHosts(List<String> allowedHosts) { this.allowedHosts = allowedHosts; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public long getBaseDelayMs() { return baseDelayMs; }
    public void setBaseDelayMs(long baseDelayMs) { this.baseDelayMs = baseDelayMs; }
    public long getMaxDelayMs() { return maxDelayMs; }
    public void setMaxDelayMs(long maxDelayMs) { this.maxDelayMs = maxDelayMs; }
    public long getRequestTimeoutMs() { return requestTimeoutMs; }
    public void setRequestTimeoutMs(long requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }
    public long getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(long connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public long getLeaseSeconds() { return leaseSeconds; }
    public void setLeaseSeconds(long leaseSeconds) { this.leaseSeconds = leaseSeconds; }
}
