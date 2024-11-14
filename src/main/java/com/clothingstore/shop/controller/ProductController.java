package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.AddProductRequestDTO;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.repository.products.ProductDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.products.ProductSummaryRepositoryDTO;
import com.clothingstore.shop.service.ProductService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
            HttpServletRequest request, // 改用 HttpServletRequest 以從 cookie 中讀取 token
            @RequestBody AddProductRequestDTO productRequestDTO) {
        try {
            // 從 cookie 中提取 token
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }

            // 調用 ProductService 以添加產品
            Integer productId = productService.addProduct(token, productRequestDTO);
            return ResponseEntity.ok(
                    new ApiResponseDTO<>(true, "Product added successfully", productId)
            );
        } catch (IllegalArgumentException e) {
            // 處理授權錯誤
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        } catch (Exception e) {
            // 處理一般錯誤
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to add product", null));
        }
    }

}
