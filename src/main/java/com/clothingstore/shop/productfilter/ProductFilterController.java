package com.clothingstore.shop.productfilter;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.product.PaginatedResponse;
import com.clothingstore.shop.dto.response.product.ProductSummaryV2ResponseDTO;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.Map;

@RestController
@RequestMapping("/api/products/filters")
public class ProductFilterController {
    private final ProductFilterService service;

    public ProductFilterController(ProductFilterService service) {
        this.service = service;
    }

    @GetMapping("/schema")
    public ResponseEntity<ApiResponseDTO<List<FilterDefinition>>> schema(@RequestParam String category) {
        try {
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Filter schema retrieved", service.schema(category)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponseDTO<PaginatedResponse<ProductSummaryV2ResponseDTO>>> search(
            @RequestBody ProductFilterRequest request) {
        try {
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Products filtered", service.search(request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @PutMapping("/{productId}/attributes")
    public ResponseEntity<ApiResponseDTO<Boolean>> updateAttributes(HttpServletRequest request,
                                                                      @PathVariable int productId,
                                                                      @RequestBody Map<String, String> attributes) {
        try {
            service.updateAttributes(TokenUtils.extractTokenFromCookies(request), productId, attributes);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Product filter attributes updated", true));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponseDTO<>(false, e.getReason(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }
}
