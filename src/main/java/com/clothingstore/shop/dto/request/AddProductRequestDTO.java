package com.clothingstore.shop.dto.request;

import com.clothingstore.shop.dto.repository.products.ProductVariantRepositoryDTO;

import java.util.List;

public class AddProductRequestDTO {
    private Integer productId;
    private String name;
    private String description;
    private String imageUrl;
    private String category;
    private Boolean isList;
    private Integer fkVendorId;
    private List<ProductVariantRepositoryDTO> productVariants;

    // Getters and Setters for each field
    public Integer getProductId() {return this.productId;}
    public void setProductId(Integer productId) {this.productId = productId;}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Boolean getIsList() { return isList; }
    public void setIsList(Boolean isList) { this.isList = isList; }

    public Integer getFkVendorId() { return fkVendorId; }
    public void setFkVendorId(Integer fkVendorId) { this.fkVendorId = fkVendorId; }

    public List<ProductVariantRepositoryDTO> getProductVariants() { return productVariants; }
    public void setProductVariants(List<ProductVariantRepositoryDTO> productVariants) { this.productVariants = productVariants; }
}