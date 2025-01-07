package com.clothingstore.shop.service;

import com.clothingstore.shop.components.TemporaryOrderStorage;
import com.clothingstore.shop.dto.others.checkout.*;
import com.clothingstore.shop.dto.others.discount.DiscountDetailsDTO;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryOrder;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryProductVariant;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryStoreOrder;
import com.clothingstore.shop.dto.request.checkout.ConfirmAmountRequestDTO;
import com.clothingstore.shop.dto.request.checkout.SubmitOrderRequestDTO;
import com.clothingstore.shop.dto.response.checkout.ConfirmAmountResponseDTO;
import com.clothingstore.shop.dto.response.checkout.SubmitOrderResponseDTO;
import com.clothingstore.shop.enums.PayStatus;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.repository.CheckoutRepository;
import com.clothingstore.shop.repository.DiscountRepository;
import com.clothingstore.shop.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CheckoutService {
    private final JwtService jwtService;
    private final CheckoutRepository checkoutRepository;
    private final TemporaryOrderStorage temporaryOrderStorage;
    private final InventoryRepository inventoryRepository;
    private final DiscountService discountService;
    private final DiscountRepository discountRepository;

    @Autowired
    public CheckoutService(JwtService jwtService, CheckoutRepository checkoutRepository, TemporaryOrderStorage temporaryOrderStorage, InventoryRepository inventoryRepository, @Lazy DiscountService discountService, DiscountRepository discountRepository) {
        this.jwtService = jwtService;
        this.checkoutRepository = checkoutRepository;
        this.temporaryOrderStorage = temporaryOrderStorage;
        this.inventoryRepository = inventoryRepository;
        this.discountService = discountService;
        this.discountRepository = discountRepository;
    }
    public ConfirmAmountResponseDTO saveTemporaryOrder(ConfirmAmountRequestDTO requestDTO, String jwtToken) throws SharedException {
        Integer customerId = jwtService.extractUserId(jwtToken);
        // 驗證訂單
        validateOrder(requestDTO);

        // 開始預留庫存
        for (CheckoutBaseStoreOrderModel storeOrder : requestDTO.getStore_orders()) {
            for (CheckoutBaseProductVariantModel productVariant : storeOrder.getProduct_variants()) {
                inventoryRepository.reserveStock(productVariant.getProduct_variant_id(), productVariant.getQuantity());
            }
        }

        // 計算暫存訂單
        TemporaryOrder tempOrder = calculateTemporaryOrder(requestDTO, customerId);

        // 存入 RAM，返回訂單 ID

        return new ConfirmAmountResponseDTO(tempOrder.getSubtotal(), tempOrder.getShippingFee(), tempOrder.getShippingDiscountAmount()+ tempOrder.getTotalStoreDiscountAmount(),tempOrder.getTotalAmount(), temporaryOrderStorage.saveTemporaryOrder(tempOrder));
    }

    private TemporaryOrder calculateTemporaryOrder(ConfirmAmountRequestDTO requestDTO, Integer customerId) throws SharedException {
        TemporaryOrder tempOrder = new TemporaryOrder(discountRepository);
//        TemporaryOrder tempOrder = discountService.calculateDiscounts(requestDTO, customerId);
        tempOrder.setCustomerId(customerId);
        tempOrder.setStoreOrders(new ArrayList<>());

        Integer totalAmount = 0;
        Integer subtotal = 0;
        Integer totalStoreDiscount = 0;
        // 計算折扣
        DiscountDetailsDTO shippingDiscount = discountService.getShippingDiscount(requestDTO.getShipping_discount_code(),customerId);
        Integer shippingDiscountAmount = discountService.calculateShippingDiscount(shippingDiscount, checkoutRepository.queryShippingFee());
        // for 所有商家訂單
        for (CheckoutBaseStoreOrderModel storeOrder : requestDTO.getStore_orders()) {
            TemporaryStoreOrder tempStoreOrder = new TemporaryStoreOrder(discountRepository);
            tempStoreOrder.setStoreId(storeOrder.getStore_id());
            tempStoreOrder.setProductVariants(new ArrayList<>());

            Integer storeSubtotal = 0;
            Integer storeDiscountAmount = 0;

            // 取得商店折扣
            DiscountDetailsDTO storeDiscount = discountService.getStoreDiscount(storeOrder,customerId);
            if(storeDiscount != null){
                tempStoreOrder.setDiscountDetails(storeDiscount);
            }
            // for 商家訂單內的order_item
            for (CheckoutBaseProductVariantModel productVariant : storeOrder.getProduct_variants()) {
                Integer unitPrice = inventoryRepository.getUnitPrice(productVariant.getProduct_variant_id());
                Integer productTotal = unitPrice * productVariant.getQuantity();

                // 累加商店金額
                storeSubtotal += productTotal;

                // 建立物件
                TemporaryProductVariant tempVariant = new TemporaryProductVariant();
                tempVariant.setProductVariantId(productVariant.getProduct_variant_id());
                tempVariant.setUnitPrice(unitPrice);
                tempVariant.setQuantity(productVariant.getQuantity());
                tempVariant.setTotalAmount(productTotal);
                tempStoreOrder.getProductVariants().add(tempVariant);
            }

            // 計算商店折扣
            if (storeDiscount != null) {
                storeDiscountAmount = discountService.calculateStoreDiscount(storeOrder,customerId);
            }

            // 設定商店訂單金額
            tempStoreOrder.setSubtotal(storeSubtotal);
            tempStoreOrder.setDiscountAmount(storeDiscountAmount);
            tempStoreOrder.setTotalAmount(storeSubtotal - storeDiscountAmount);
            totalStoreDiscount+=storeDiscountAmount;

            // 累加訂單總金額
            subtotal += storeSubtotal;
            totalAmount += storeSubtotal - storeDiscountAmount;

            // 加入臨時訂單
            tempOrder.getStoreOrders().add(tempStoreOrder);
        }

        // 更新訂單
        totalAmount += checkoutRepository.queryShippingFee();
        totalAmount -= shippingDiscountAmount;
        tempOrder.setTotalAmount(totalAmount);
        tempOrder.setSubtotal(subtotal);
        tempOrder.setShippingDiscountAmount(shippingDiscountAmount);
        if(shippingDiscount != null){
            tempOrder.setShippingDiscountCode(shippingDiscount.getCode());
        }
        tempOrder.setShippingFee(checkoutRepository.queryShippingFee());
        tempOrder.setTotalStoreDiscountAmount(totalStoreDiscount);
        return tempOrder;
    }


    public void validateOrder(ConfirmAmountRequestDTO requestDTO) throws SharedException {
        // 檢查訂單是否有商品
        if (requestDTO.getStore_orders() == null || requestDTO.getStore_orders().isEmpty()) {
            throw new SharedException("Order is empty");
        }

        // 檢查訂單商品是否有數量
        for (CheckoutBaseStoreOrderModel storeOrder : requestDTO.getStore_orders()) {
            if (storeOrder.getProduct_variants() == null || storeOrder.getProduct_variants().isEmpty()) {
                throw new SharedException("Order is empty");
            }
        }
    }

    public SubmitOrderResponseDTO confirmOrder(SubmitOrderRequestDTO requestDTO, String customerId) throws SharedException {
        // 取出暫存訂單
        TemporaryOrder tempOrder = temporaryOrderStorage.getTemporaryOrder(requestDTO.getOrder_id());
        if (tempOrder == null) {
            throw new SharedException("Temporary order expired or not found");
        }

        // 確認庫存，扣減正式庫存
        for (TemporaryStoreOrder storeOrder : tempOrder.getStoreOrders()) {
            for (TemporaryProductVariant productVariant : storeOrder.getProductVariants()) {
                inventoryRepository.finalizeStock(productVariant.getProductVariantId(), productVariant.getQuantity());
            }
        }

        // 將訂單寫入數據庫
        Integer orderId = checkoutRepository.saveOrder( requestDTO, tempOrder, tempOrder.getCustomerId());

        // 刪除暫存訂單
        temporaryOrderStorage.confirmOrder(requestDTO.getOrder_id());

        return new SubmitOrderResponseDTO(orderId, PayStatus.PENDING.toString(), tempOrder.getTotalAmount(),tempOrder.getTotalStoreDiscountAmount()+tempOrder.getShippingDiscountAmount(),tempOrder.getTotalAmount(),"2024/12/31 23:59:59");
    }

    public String cancelOrder(String tempOrderId) throws SharedException {
        TemporaryOrder tempOrder = temporaryOrderStorage.getTemporaryOrder(tempOrderId);
        if (tempOrder == null) {
            throw new SharedException("Temporary order expired or not found");
        }

        // 回滾預留庫存
        for (TemporaryStoreOrder storeOrder : tempOrder.getStoreOrders()) {
            for (TemporaryProductVariant productVariant : storeOrder.getProductVariants()) {
                inventoryRepository.rollbackStock(productVariant.getProductVariantId(), productVariant.getQuantity());
            }
        }

        // 刪除暫存訂單
        temporaryOrderStorage.confirmOrder(tempOrderId);
        return "Order canceled successfully";
    }
}