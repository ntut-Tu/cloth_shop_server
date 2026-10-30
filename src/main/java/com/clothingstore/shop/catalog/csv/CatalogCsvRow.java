package com.clothingstore.shop.catalog.csv;

public record CatalogCsvRow(Operation operation, Integer productId, Integer variantId,
                            String name, String description, String imageUrl, String category,
                            Boolean listed, String color, String size, Integer price, Integer stock) {
    public enum Operation { ADD, EDIT, DELETE }
}
