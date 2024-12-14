package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.repository.products.ProductDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.products.ProductSummaryRepositoryDTO;
import com.clothingstore.shop.dto.request.AddProductRequestDTO;
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

    public List<ProductSummaryRepositoryDTO> getProductSummaries(int page, int pageSize) {
        return productRepository.fetchProductSummaries(page, pageSize);
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

    public List<ProductSummaryRepositoryDTO> getProductSummariesByCategory(String category, int page, int pageSize) {
        return productRepository.fetchProductSummariesByCategory(category, page, pageSize);
    }

    public List<ProductSummaryRepositoryDTO> searchProductSummaries(String target, int page, int pageSize) {
        return productRepository.searchProductSummaries(target, page, pageSize);
    }

    public List<ProductSummaryRepositoryDTO> getProductSummariesOrderBy(String method, int page, int pageSize) throws SharedException {
        try{
            return productRepository.fetchProductSummariesOrderBy(method, page, pageSize);
        }catch (Exception e){
            throw e;
        }
    }
}
