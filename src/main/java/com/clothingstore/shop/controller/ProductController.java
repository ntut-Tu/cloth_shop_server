package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.AddProductRequestDTO;
import com.clothingstore.shop.dto.request.product.FetchProductsParams;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.repository.products.ProductDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.products.ProductSummaryRepositoryDTO;
import com.clothingstore.shop.dto.response.product.PaginatedResponse;
import com.clothingstore.shop.dto.response.product.ProductInfoResponseDTO;
import com.clothingstore.shop.dto.response.product.ProductSummaryV2ResponseDTO;
import com.clothingstore.shop.service.ProductService;
import com.clothingstore.shop.utils.TokenUtils;
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
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    /**
     * 获取商品列表，支持分类、排序和搜索
     *
     * @param page     页码（默认为1）
     * @param pageSize 每页条数（默认为30）
     * @param category 商品分类
     * @param sort     排序条件（如price_asc, price_desc）
     * @param search   搜索关键字
     * @return 包含商品摘要的响应
     */
    @GetMapping("/v2")
    public ResponseEntity<ApiResponseDTO<PaginatedResponse<ProductSummaryV2ResponseDTO>>> fetchProductsV2(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam String role
    ) {
        try {
            FetchProductsParams fetchParams = new FetchProductsParams(page, pageSize, category, sort, search,role);
            String token = TokenUtils.extractTokenFromCookies(request);
            PaginatedResponse<ProductSummaryV2ResponseDTO> products;
            switch (fetchParams.getRole()){
                case "admin":
                    if (token == null) {
                        throw new IllegalArgumentException("Token not found");
                    }
                    products = productService.fetchAdminProductsV2(token,fetchParams);
                    break;
                case "vendor":
                    if (token == null) {
                        throw new IllegalArgumentException("Token not found");
                    }
                    products = productService.fetchVendorProductsV2(token,fetchParams);
                    break;
                default:
                    products = productService.fetchProductsV2(fetchParams);
                    break;
            }
            return ResponseEntity.ok(new ApiResponseDTO<>(true,"Product v2 fetch successfully",products));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/product-list-for-coupon")
    public ResponseEntity<ApiResponseDTO<List<ProductInfoResponseDTO>>> getProductListForCoupon(
            HttpServletRequest request
    ) {
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            List<ProductInfoResponseDTO> productList = productService.getProductListForCoupon(token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Product list for coupon retrieved successfully", productList));
        }catch (Exception e){
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/update-status/{productVariantId}")
    public ResponseEntity<ApiResponseDTO<Boolean>> updateProductStatus(
            HttpServletRequest request,
            @PathVariable  Integer productVariantId,
            @RequestBody  boolean updatedStatus
    ) {
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            productService.updateProductStatus(token, productVariantId,updatedStatus);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Product status updated successfully", true));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/update-stock/{productVariantId}")
    public ResponseEntity<ApiResponseDTO<Boolean>> updateProductStock(
            HttpServletRequest request,
            @PathVariable  Integer productVariantId ,
            @RequestBody Integer newStock
    ) {
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            productService.updateProductStock(token, productVariantId, newStock);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Product stock updated successfully", true));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }
}
