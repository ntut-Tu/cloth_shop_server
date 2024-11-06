package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.AddProductRequestDTO;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.repository.products.ProductDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.products.ProductSummaryRepositoryDTO;
import com.clothingstore.shop.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/summaries")
    public ApiResponseDTO<List<ProductSummaryRepositoryDTO>> getProductSummaries(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int pageSize) {

        List<ProductSummaryRepositoryDTO> productSummaries = productService.getProductSummaries(page, pageSize);

        return new ApiResponseDTO<>(true, "Product summaries retrieved successfully", productSummaries);
    }

    @GetMapping("/details/{productId}")
    public ApiResponseDTO<ProductDetailRepositoryDTO> getProductDetails(@PathVariable int productId) {
        ProductDetailRepositoryDTO productDetailRepositoryDTO = productService.getProductDetails(productId);
        return new ApiResponseDTO<>(true, "Product details retrieved successfully", productDetailRepositoryDTO);
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO<Integer>> addProduct(
            @RequestHeader("Authorization") String token,
            @RequestBody AddProductRequestDTO productRequestDTO) {
        try {
            // Call the ProductService to add the product
            Integer productId = productService.addProduct(token.replace("Bearer ", ""), productRequestDTO);
            return ResponseEntity.ok(
                    new ApiResponseDTO<>(true, "Product added successfully", productId)
            );
        } catch (IllegalArgumentException e) {
            // Handle authorization error
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        } catch (Exception e) {
            // Handle general errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to add product", null));
        }
    }
}
