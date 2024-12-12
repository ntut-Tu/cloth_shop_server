package com.clothingstore.shop.enums;

public enum CategorizedProduct {
    ALL("All"),
    SHIRT("Cloth"),
    PANTS("Pants"),
    SHOES("Shoes"),
    ACCESSORIES("Accessories");

    private final String category;
    CategorizedProduct(String category) {
        this.category = category;
    }
    public String getCategory() {
        return category;
    }
}
