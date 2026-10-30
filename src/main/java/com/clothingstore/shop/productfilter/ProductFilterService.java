package com.clothingstore.shop.productfilter;

import com.clothingstore.shop.dto.response.product.PaginatedResponse;
import com.clothingstore.shop.dto.response.product.ProductSummaryV2ResponseDTO;
import com.clothingstore.shop.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class ProductFilterService {
    private final ProductFilterRegistry registry;
    private final ProductFilterValidator validator;
    private final ProductFilterPlanner planner;
    private final ProductFilterRepository repository;
    private final JwtService jwtService;

    public ProductFilterService(ProductFilterRegistry registry, ProductFilterValidator validator,
                                ProductFilterPlanner planner, ProductFilterRepository repository, JwtService jwtService) {
        this.registry = registry;
        this.validator = validator;
        this.planner = planner;
        this.repository = repository;
        this.jwtService = jwtService;
    }

    public List<FilterDefinition> schema(String category) {
        return registry.forCategory(category);
    }

    public PaginatedResponse<ProductSummaryV2ResponseDTO> search(ProductFilterRequest request) {
        return repository.search(planner.plan(validator.validate(request)));
    }

    @Transactional
    public void updateAttributes(String token, int productId, Map<String, String> attributes) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vendor authentication is required");
        }
        if (productId < 1 || attributes == null || attributes.isEmpty() || attributes.size() > 20) {
            throw new IllegalArgumentException("A product and 1–20 attributes are required");
        }
        int userId;
        try {
            if (!"vendor".equals(jwtService.extractRole(token))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vendor role is required");
            }
            userId = jwtService.extractUserId(token);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token", e);
        }
        String category = repository.categoryOwnedBy(productId, userId);
        if (category == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            FilterDefinition definition = registry.resolve(category, attribute.getKey());
            if (definition.source() != FilterSource.CUSTOM) {
                throw new IllegalArgumentException("Not a custom attribute: " + attribute.getKey());
            }
            String value = attribute.getValue();
            if (value != null && (value.isBlank() || value.trim().length() > 200)) {
                throw new IllegalArgumentException("Custom attribute must contain 1–200 characters: " + attribute.getKey());
            }
        }
        attributes.forEach((key, value) -> {
            if (value == null) {
                repository.deleteAttribute(productId, key);
            } else {
                repository.upsertAttribute(productId, key, value.trim());
            }
        });
    }
}
