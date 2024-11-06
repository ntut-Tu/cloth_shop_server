package com.clothingstore.shop.dto.repository.products;

import java.math.BigDecimal;
import java.util.List;

public class ProductDetailRepositoryDTO {
    private Integer productId;
    private String name;
    private String description;
    private Integer totalSales;
    private BigDecimal rate;
    private String imageUrl;
    private String category;
    private Boolean isList;
    private Integer fkReviewId;
    private Integer fkVendorId;
    private String storeDescription;
    private String storeAddress;
    private String storeLogoUrl;
    private List<ProductVariantRepositoryDTO> productVariantRepositoryDTO;
    // Getters and Setters
    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(Integer totalSales) {
        this.totalSales = totalSales;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIsList() {
        return isList;
    }

    public void setIsList(Boolean isList) {
        this.isList = isList;
    }

    public Integer getFkReviewId() {
        return fkReviewId;
    }

    public void setFkReviewId(Integer fkReviewId) {
        this.fkReviewId = fkReviewId;
    }

    public Integer getFkVendorId() {
        return fkVendorId;
    }

    public void setFkVendorId(Integer fkVendorId) {
        this.fkVendorId = fkVendorId;
    }

    public String getStoreDescription() {
        return storeDescription;
    }

    public void setStoreDescription(String storeDescription) {
        this.storeDescription = storeDescription;
    }

    public String getStoreAddress() {
        return storeAddress;
    }

    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    public String getStoreLogoUrl() {
        return storeLogoUrl;
    }

    public void setStoreLogoUrl(String storeLogoUrl) {
        this.storeLogoUrl = storeLogoUrl;
    }

    public List<ProductVariantRepositoryDTO> getProductVariantRepositoryDTO() { return this.productVariantRepositoryDTO; }

    public void setProductVariantRepositoryDTO(List<ProductVariantRepositoryDTO> productVariantRepositoryDTO) {this.productVariantRepositoryDTO = productVariantRepositoryDTO; }
}
