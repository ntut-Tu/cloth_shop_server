//package com.clothingstore.shop.dto.others.tempOrder;
//
//import com.clothingstore.shop.dto.others.discount.DiscountDetailsDTO;
//import com.clothingstore.shop.dto.others.discount.SpecialDiscountDTO;
//import com.clothingstore.shop.exceptions.SharedException;
//import com.clothingstore.shop.repository.DiscountRepository;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class StoreOrderCalculationResult {
//    private final DiscountRepository discountRepository;
//    private Integer storeId; // 商店 ID
//    private Integer subtotal; // 商店小计金额
//    private Integer discountAmount; // 商店折扣金额
//    private Integer totalAmount; // 商店总金额（折扣后）
//    private List<ProductCalculationResult> productDetails; // 商品级别详情
//    private DiscountDetailsDTO discountDetails; // 折扣详情
//    // 构造方法
//    public StoreOrderCalculationResult(DiscountRepository discountRepository) {
//        this.discountRepository = discountRepository;
//        this.productDetails = new ArrayList<>();
//    }
//
//    // Getters 和 Setters
//    public Integer getStoreId() {
//        return storeId;
//    }
//
//    public void setStoreId(Integer storeId) {
//        this.storeId = storeId;
//    }
//
//    public Integer getSubtotal() {
//        return subtotal;
//    }
//
//    public void setSubtotal(Integer subtotal) {
//        this.subtotal = subtotal;
//    }
//
//    public Integer getDiscountAmount() {
//        return discountAmount;
//    }
//
//    public void setDiscountAmount(Integer discountAmount) {
//        this.discountAmount = discountAmount;
//    }
//
//    public Integer getTotalAmount() {
//        return totalAmount;
//    }
//
//    public void setTotalAmount(Integer totalAmount) {
//        this.totalAmount = totalAmount;
//    }
//
//    public List<ProductCalculationResult> getProductDetails() {
//        return productDetails;
//    }
//
//    public void setProductDetails(List<ProductCalculationResult> productDetails) {
//        this.productDetails = productDetails;
//    }
//
//    // 添加商品详情
//    public void addProductDetail(ProductCalculationResult productDetail) {
//        this.productDetails.add(productDetail);
//    }
//
//    public void setDiscountDetails(DiscountDetailsDTO storeDiscount) {
//        this.discountDetails = storeDiscount;
//    }
//    public DiscountDetailsDTO getDiscountDetails() {
//        return discountDetails;
//    }
//
//    public Integer getSpecialDiscountId() throws SharedException {
//        if(discountDetails instanceof SpecialDiscountDTO) {
//            return discountRepository.queryDiscountIdByCode(discountDetails.getCode());
//        } else {
//            return null;
//        }
//    }
//
//    public Integer getSeasonalDiscountId() throws SharedException {
//        if(discountDetails instanceof SpecialDiscountDTO) {
//            return discountRepository.queryDiscountIdByCode(discountDetails.getCode());
//        } else {
//            return null;
//        }
//    }
//}
//
