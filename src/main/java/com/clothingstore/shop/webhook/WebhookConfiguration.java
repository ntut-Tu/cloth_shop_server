package com.clothingstore.shop.webhook;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(WebhookProperties.class)
public class WebhookConfiguration {
    @Bean
    CloseableHttpClient webhookHttpClient(WebhookProperties properties, PublicWebhookDnsResolver dnsResolver) {
        var connections = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(dnsResolver)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectTimeoutMs()))
                        .setSocketTimeout(Timeout.ofMilliseconds(properties.getRequestTimeoutMs()))
                        .build())
                .build();
        return HttpClients.custom()
                .setConnectionManager(connections)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setResponseTimeout(Timeout.ofMilliseconds(properties.getRequestTimeoutMs()))
                        .setConnectionRequestTimeout(Timeout.ofMilliseconds(properties.getConnectTimeoutMs()))
                        .build())
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .disableCookieManagement()
                .build();
    }
}
