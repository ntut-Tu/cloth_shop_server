//package com.clothingstore.shop.dto.others.tempOrder;
//
//import com.clothingstore.shop.dto.others.discount.DiscountDetailsDTO;
//import com.clothingstore.shop.exceptions.SharedException;
//import com.clothingstore.shop.repository.DiscountRepository;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//public class OrderCalculationResult {
//    private final DiscountRepository discountRepository;
//    // 总订单级别数据
//    private Integer totalAmount; // 总金额（折扣后）
//    private Integer subtotal; // 小计（未折扣）
//    private Integer shippingDiscountAmount; // 运费折扣金额
//    private String shippingDiscountCode; // 运费折扣 ID
//    // 商店订单级别数据
//    private List<StoreOrderCalculationResult> storeOrders; // 商店级订单集合
//
//    // 构造方法
//    public OrderCalculationResult(DiscountRepository discountRepository) {
//        this.discountRepository = discountRepository;
//        this.storeOrders = new ArrayList<>();
//    }
//
//
//    // Getters 和 Setters
//    public Integer getTotalAmount() {
//        return totalAmount;
//    }
//
//    public void setTotalAmount(Integer totalAmount) {
//        this.totalAmount = totalAmount;
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
//    public Integer getShippingDiscountAmount() {
//        return shippingDiscountAmount;
//    }
//
//    public void setShippingDiscountAmount(Integer shippingDiscountAmount) {
//        this.shippingDiscountAmount = shippingDiscountAmount;
//    }
//
//    public List<StoreOrderCalculationResult> getStoreOrders() {
//        return storeOrders;
//    }
//
//    public void setStoreOrders(List<StoreOrderCalculationResult> storeOrders) {
//        this.storeOrders = storeOrders;
//    }
//
//    // 添加商店订单结果
//    public void addStoreOrder(StoreOrderCalculationResult storeOrder) {
//        this.storeOrders.add(storeOrder);
//    }
//
//    public void addStoreDiscount(int storeId, DiscountDetailsDTO storeDiscount) {
//        for (StoreOrderCalculationResult storeOrder : storeOrders) {
//            if (storeOrder.getStoreId().equals(storeId)) {
//                storeOrder.setDiscountDetails(storeDiscount);
//                break;
//            }
//        }
//    }
//
//    public Integer getShippingDiscountId() throws SharedException {
//        return discountRepository.queryDiscountIdByCode(shippingDiscountCode);
//    }
//
//    public void setShippingDiscountCode(String shippingDiscountCode) {
//        this.shippingDiscountCode = shippingDiscountCode;
//    }
//}
//
