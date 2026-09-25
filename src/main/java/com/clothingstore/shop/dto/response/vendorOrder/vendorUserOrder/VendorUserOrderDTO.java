package com.clothingstore.shop.dto.response.vendorOrder.vendorUserOrder;

import java.util.List;

public class VendorUserOrderDTO {
    private Integer productId;
    private String productName;
    private List<VendorProductVariantDTO> productVariants;

    // Getters and Setters

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public List<VendorProductVariantDTO> getProductVariants() {
        return productVariants;
    }

    public void setProductVariants(List<VendorProductVariantDTO> productVariants) {
        this.productVariants = productVariants;
    }

}
