package com.clothingstore.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductApiIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>(
            System.getProperty("postgres.image", "postgres:16-alpine"));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
    }

    @Autowired TestRestTemplate http;

    @Test
    void guestProductSearchReturnsPersistedDemoProductAndVariantPrice() {
        ResponseEntity<JsonNode> response = http.getForEntity(
                "/api/products/v2?role=guest&search=Demo+Cloth", JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.path("status").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalRecords").asInt()).isEqualTo(1);
        JsonNode items = body.path("data").path("items");
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isEqualTo(1);
        JsonNode product = items.get(0);
        assertThat(product.path("name").asText()).isEqualTo("Demo Cloth");
        assertThat(product.path("category").asText()).isEqualTo("Cloth");
        assertThat(product.path("isActive").asBoolean()).isTrue();
        assertThat(product.path("minPrice").asInt()).isEqualTo(250);
        assertThat(product.path("maxPrice").asInt()).isEqualTo(250);
    }
}
