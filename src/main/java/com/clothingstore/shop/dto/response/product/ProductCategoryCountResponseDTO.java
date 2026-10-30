package com.clothingstore.shop.dto.response.product;

public class ProductCategoryCountResponseDTO {
    private final String category;
    private final int count;

    public ProductCategoryCountResponseDTO(String category, int count) {
        this.category = category;
        this.count = count;
    }

    public String getCategory() {
        return category;
    }

    public int getCount() {
        return count;
    }
}
