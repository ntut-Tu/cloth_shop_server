package com.clothingstore.shop.dto.repository.products;

public class ProductVariantRepositoryDTO {
    private Integer productVariantId;
    private String color;
    private Integer stock;
    private String size;
    private Integer price;

    public void setColor(String color) {this.color = color;}
    public void setStock(Integer stock) {this.stock = stock;}
    public void setSize(String size) {this.size = size;}
    public void setPrice(Integer price) {this.price = price;}
    public void setProductVariantId(Integer productVariantId) {this.productVariantId = productVariantId;}

    public Integer getProductVariantId() {return productVariantId;}
    public String getColor() {return color;}
    public Integer getStock() {return stock;}
    public String getSize() {return size;}
    public Integer getPrice() {return price;}

}
