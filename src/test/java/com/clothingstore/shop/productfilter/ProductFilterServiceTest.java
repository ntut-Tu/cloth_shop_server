package com.clothingstore.shop.productfilter;

import com.clothingstore.shop.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductFilterServiceTest {
    private final ProductFilterRepository repository = mock(ProductFilterRepository.class);
    private final JwtService jwt = mock(JwtService.class);

    @Test
    void vendorCanUpsertOnlyConfiguredCustomAttribute() {
        ProductFilterService service = service();
        when(jwt.extractRole("token")).thenReturn("vendor");
        when(jwt.extractUserId("token")).thenReturn(7);
        when(repository.categoryOwnedBy(9, 7)).thenReturn("Shoes");

        service.updateAttributes("token", 9, Map.of("closure", "  Lace  "));

        verify(repository).upsertAttribute(9, "closure", "Lace");
    }

    @Test
    void vendorCanRemoveCustomAttribute() {
        ProductFilterService service = service();
        when(jwt.extractRole("token")).thenReturn("vendor");
        when(jwt.extractUserId("token")).thenReturn(7);
        when(repository.categoryOwnedBy(9, 7)).thenReturn("Shoes");
        java.util.Map<String, String> update = new java.util.HashMap<>();
        update.put("closure", null);

        service.updateAttributes("token", 9, update);

        verify(repository).deleteAttribute(9, "closure");
    }

    @Test
    void refusesUnownedProduct() {
        ProductFilterService service = service();
        when(jwt.extractRole("token")).thenReturn("vendor");
        when(jwt.extractUserId("token")).thenReturn(7);

        assertThatThrownBy(() -> service.updateAttributes("token", 9, Map.of("closure", "Lace")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
        verify(repository, never()).upsertAttribute(9, "closure", "Lace");
    }

    @Test
    void refusesNonCustomAttributeEvenForOwner() {
        ProductFilterService service = service();
        when(jwt.extractRole("token")).thenReturn("vendor");
        when(jwt.extractUserId("token")).thenReturn(7);
        when(repository.categoryOwnedBy(9, 7)).thenReturn("Shoes");

        assertThatThrownBy(() -> service.updateAttributes("token", 9, Map.of("price", "1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a custom attribute");
    }

    private ProductFilterService service() {
        try {
            org.springframework.core.env.StandardEnvironment environment = new org.springframework.core.env.StandardEnvironment();
            environment.getPropertySources().addFirst(new org.springframework.boot.env.YamlPropertySourceLoader()
                    .load("catalog", new org.springframework.core.io.ClassPathResource("application.yml")).get(0));
            ProductFilterProperties properties = org.springframework.boot.context.properties.bind.Binder.get(environment)
                    .bind("catalog.filters", org.springframework.boot.context.properties.bind.Bindable.of(ProductFilterProperties.class))
                    .orElseThrow(() -> new IllegalStateException("Filter configuration is missing"));
            ProductFilterRegistry registry = new ProductFilterRegistry(properties);
            return new ProductFilterService(registry, new ProductFilterValidator(registry),
                    new ProductFilterPlanner(), repository, jwt);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
