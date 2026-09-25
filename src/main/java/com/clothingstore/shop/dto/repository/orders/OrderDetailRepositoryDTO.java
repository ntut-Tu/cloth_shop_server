package com.clothingstore.shop.dto.repository.orders;

public class OrderDetailRepositoryDTO {
    Integer order_item_id;
    Integer unit_price;
    Integer quantity;
    Integer total_price;
    String order_image_url;
    String product_name;
    String size;
    String color;

    public Integer getOrderItemId() {
        return order_item_id;
    }

    public void setOrderItemId(Integer order_item_id) {
        this.order_item_id = order_item_id;
    }

    public Integer getUnitPrice() {
        return unit_price;
    }

    public void setUnitPrice(Integer unit_price) {
        this.unit_price = unit_price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getTotalPrice() {
        return total_price;
    }

    public void setTotalPrice(Integer total_price) {
        this.total_price = total_price;
    }

    public String getOrderImageUrl() {
        return order_image_url;
    }

    public void setOrderImageUrl(String order_image_url) {
        this.order_image_url = order_image_url;
    }

    public String getProductName() {
        return product_name;
    }

    public void setProductName(String product_name) {
        this.product_name = product_name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
