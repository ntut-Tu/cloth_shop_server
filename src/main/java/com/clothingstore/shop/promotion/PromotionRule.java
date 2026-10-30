package com.clothingstore.shop.promotion;

import java.util.List;

/**
 * A JSON rule tree. Composite nodes use children (ALL/ANY) or child (NOT);
 * leaf nodes use only the fields required by their type.
 */
public class PromotionRule {
    private String type;
    private List<PromotionRule> children;
    private PromotionRule child;
    private Integer amount;
    private Integer quantity;
    private Integer variantId;
    private Integer vendorId;
    private String category;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<PromotionRule> getChildren() { return children; }
    public void setChildren(List<PromotionRule> children) { this.children = children; }
    public PromotionRule getChild() { return child; }
    public void setChild(PromotionRule child) { this.child = child; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getVariantId() { return variantId; }
    public void setVariantId(Integer variantId) { this.variantId = variantId; }
    public Integer getVendorId() { return vendorId; }
    public void setVendorId(Integer vendorId) { this.vendorId = vendorId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
