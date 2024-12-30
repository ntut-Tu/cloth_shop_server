package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.repository.products.ProductDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.products.ProductSummaryRepositoryDTO;
import com.clothingstore.shop.dto.request.AddProductRequestDTO;
import com.clothingstore.shop.dto.request.product.FetchProductsParams;
import com.clothingstore.shop.dto.response.product.PaginatedResponse;
import com.clothingstore.shop.dto.response.product.ProductInfoResponseDTO;
import com.clothingstore.shop.dto.response.product.ProductSummaryV2ResponseDTO;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final JwtService jwtService;
    private final AuthService authService;

    @Autowired
    public ProductService(ProductRepository productRepository, JwtService jwtService, AuthService authService) {
        this.productRepository = productRepository;
        this.jwtService = jwtService;
        this.authService = authService;
    }

    public ProductDetailRepositoryDTO getProductDetails(int productId) {
        return productRepository.fetchProductDetails(productId);
    }

    public Integer addProduct(String token, AddProductRequestDTO productRequestDTO) {
        // Step 1: Extract userId from the JWT token to verify vendor identity
        Integer userId = jwtService.extractUserId(token);

        // Check if the user ID matches the vendor ID in the productDTO
        if (!authService.checkUserExists(userId,"vendor")) {
            throw new IllegalArgumentException("User is not authorized to add products for this vendor.");
        }
        productRequestDTO.setFkVendorId(authService.getVendorId(userId));
        // Step 2: Add the product using ProductRepository
        return productRepository.addProduct(productRequestDTO);
    }

    public PaginatedResponse<ProductSummaryV2ResponseDTO> fetchProductsV2(FetchProductsParams fetchParams) {
        return productRepository.fetchProductsV2(fetchParams,null,"customer");
    }

    public PaginatedResponse<ProductSummaryV2ResponseDTO> fetchAdminProductsV2(String token,FetchProductsParams fetchParams) {
        Integer userId = jwtService.extractUserId(token);
        if (!authService.checkUserExists(userId,"admin")) {
            throw new IllegalArgumentException("User is not authorized to search products.");
        }
        return productRepository.fetchProductsV2(fetchParams,userId,"admin");
    }

    public PaginatedResponse<ProductSummaryV2ResponseDTO> fetchVendorProductsV2(String token,FetchProductsParams fetchParams) {
        Integer userId = jwtService.extractUserId(token);
        if (!authService.checkUserExists(userId,"vendor")) {
            throw new IllegalArgumentException("User is not authorized to search products.");
        }
        return productRepository.fetchProductsV2(fetchParams,userId,"vendor");
    }

    public List<ProductInfoResponseDTO> getProductListForCoupon(String token) {
        Integer userId = jwtService.extractUserId(token);
        if (!authService.checkUserExists(userId,"vendor")) {
            throw new IllegalArgumentException("User is not authorized to search products.");
        }
        return productRepository.getProductListForCoupon(authService.getVendorId(userId));
    }

    public void updateProductStatus(String token, Integer productVariantId, Boolean updatedStatus) {
        Integer userId = jwtService.extractUserId(token);
        if (!authService.checkUserExists(userId,"vendor")) {
            throw new IllegalArgumentException("User is not authorized to update product status.");
        }
        productRepository.updateProductStatus(authService.getVendorId(userId),productVariantId,updatedStatus);
    }


    public void updateProductStock(String token,Integer productVariantId,Integer newStock) {
        Integer userId = jwtService.extractUserId(token);
        if (!authService.checkUserExists(userId,"vendor")) {
            throw new IllegalArgumentException("User is not authorized to update product stock.");
        }
        productRepository.updateProductStock(authService.getVendorId(userId),productVariantId,newStock);
    }
}
