package com.clothingstore.shop.catalog.csv;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.messaging.MessageChannel;

@Configuration
@EnableIntegration
@IntegrationComponentScan(basePackageClasses = CatalogCsvGateway.class)
@EnableConfigurationProperties(CatalogCsvProperties.class)
public class CatalogCsvConfiguration {
    @Bean(name = CatalogCsvChannels.IMPORT)
    public MessageChannel catalogCsvImportInput() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow catalogCsvImportFlow(CatalogCsvImportHandler handler) {
        return IntegrationFlow.from(CatalogCsvChannels.IMPORT)
                .handle(handler, "process")
                .get();
    }
}
