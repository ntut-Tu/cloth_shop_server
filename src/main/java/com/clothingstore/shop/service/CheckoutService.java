package com.clothingstore.shop.service;

import com.clothingstore.shop.components.TemporaryOrderStorage;
import com.clothingstore.shop.dto.others.checkout.*;
import com.clothingstore.shop.dto.others.discount.DiscountDetailsDTO;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryOrder;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryProductVariant;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryStoreOrder;
import com.clothingstore.shop.dto.request.checkout.ConfirmAmountRequestDTO;
import com.clothingstore.shop.dto.request.checkout.ConfirmDiscountRequestDTO;
import com.clothingstore.shop.dto.request.checkout.SubmitOrderRequestDTO;
import com.clothingstore.shop.dto.response.checkout.ConfirmAmountResponseDTO;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.manager.CheckoutManager;
import com.clothingstore.shop.repository.CheckoutRepository;
import com.clothingstore.shop.repository.DiscountRepository;
import com.clothingstore.shop.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CheckoutService {
    private final JwtService jwtService;
    private final CheckoutRepository checkoutRepository;
    private final CheckoutManager checkoutManager;
    private final TemporaryOrderStorage temporaryOrderStorage;
    private final InventoryRepository inventoryRepository;
    private final DiscountService discountService;
    private final DiscountRepository discountRepository;

    @Autowired
    public CheckoutService(JwtService jwtService, CheckoutRepository checkoutRepository, CheckoutManager checkoutManager, TemporaryOrderStorage temporaryOrderStorage, InventoryRepository inventoryRepository, DiscountService discountService, DiscountRepository discountRepository) {
        this.jwtService = jwtService;
        this.checkoutRepository = checkoutRepository;
        this.checkoutManager = checkoutManager;
        this.temporaryOrderStorage = temporaryOrderStorage;
        this.inventoryRepository = inventoryRepository;
        this.discountService = discountService;
        this.discountRepository = discountRepository;
    }
//     TODO: 應該改成 confirm 時先下一個暫時訂單，submit 時再用 id 確認完成動作，避免邏輯重複
//    public ConfirmAmountResponseDTO confirmAmount(String jwtToken, ConfirmAmountRequestDTO confirmAmountRequestDTO) throws SharedException{
//        Integer userId = jwtService.extractUserId(jwtToken);
//        //從request中提取每個商品從repository中取得的價格加總並扣掉優惠金額等完成confirmAmountResponseDTO的處理及建立
//        return null;
//    }
//
//    public DiscountDetailsDTO confirmDiscount(String jwtToken, ConfirmDiscountRequestDTO confirmDiscountRequestDTO) throws SharedException{
//        Integer userId = jwtService.extractUserId(jwtToken);
//        //處裡並完成confirmDiscountResponseDTO的建立
//        return checkoutManager.fetchDiscountDetails(confirmDiscountRequestDTO.getDiscount_code(),userId);
//    }
//
//    public Integer submitOrder(String jwtToken, SubmitOrderRequestDTO submitOrderRequestDTO) throws SharedException {
//        Integer userId = jwtService.extractUserId(jwtToken);
//        //處裡並完成submitOrderResponseDTO的建立
//        return checkoutManager.storeOrder(submitOrderRequestDTO, userId);
//    }
    // ============================== 以下為新增的程式碼 ==============================
    public String saveTemporaryOrder(ConfirmAmountRequestDTO requestDTO, String jwtToken) throws SharedException {
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
        return temporaryOrderStorage.saveTemporaryOrder(tempOrder);
    }

    private TemporaryOrder calculateTemporaryOrder(ConfirmAmountRequestDTO requestDTO, Integer customerId) throws SharedException {
        TemporaryOrder tempOrder = new TemporaryOrder(discountRepository);
        tempOrder.setCustomerId(customerId);
        tempOrder.setStoreOrders(new ArrayList<>());

        // 初始化总计
        Integer totalAmount = 0;
        Integer subtotal = 0;

        // 计算运费折扣
        DiscountDetailsDTO shippingDiscount = discountService.getShippingDiscount(requestDTO.getShipping_discount_code());
        Integer shippingDiscountAmount = discountService.calculateShippingDiscount(shippingDiscount, checkoutRepository.queryShippingFee());

        // 遍历商店订单
        for (CheckoutBaseStoreOrderModel storeOrder : requestDTO.getStore_orders()) {
            TemporaryStoreOrder tempStoreOrder = new TemporaryStoreOrder(discountRepository);
            tempStoreOrder.setStoreId(storeOrder.getStore_id());
            tempStoreOrder.setProductVariants(new ArrayList<>());

            // 初始化商店总价
            Integer storeSubtotal = 0;
            Integer storeDiscountAmount = 0;

            // 获取商店折扣
            DiscountDetailsDTO storeDiscount = discountService.getStoreDiscount(storeOrder);

            // 遍历商品
            for (CheckoutBaseProductVariantModel productVariant : storeOrder.getProduct_variants()) {
                Integer unitPrice = inventoryRepository.getUnitPrice(productVariant.getProduct_variant_id());
                Integer productTotal = unitPrice * productVariant.getQuantity();

                // 累加商店小计
                storeSubtotal += productTotal;

                // 构建临时商品数据
                TemporaryProductVariant tempVariant = new TemporaryProductVariant();
                tempVariant.setProductVariantId(productVariant.getProduct_variant_id());
                tempVariant.setUnitPrice(unitPrice);
                tempVariant.setQuantity(productVariant.getQuantity());
                tempStoreOrder.getProductVariants().add(tempVariant);
            }

            // 计算商店折扣金额
            if (storeDiscount != null) {
                storeDiscountAmount = discountService.calculateStoreDiscount(storeOrder);
            }

            // 设置商店总计和折扣
            tempStoreOrder.setSubtotal(storeSubtotal);
            tempStoreOrder.setDiscountAmount(storeDiscountAmount);

            // 更新总计和小计
            subtotal += storeSubtotal;
            totalAmount += storeSubtotal - storeDiscountAmount;

            // 添加商店订单到临时订单
            tempOrder.getStoreOrders().add(tempStoreOrder);
        }

        // 更新临时订单总计
        totalAmount -= shippingDiscountAmount;
        tempOrder.setTotalAmount(totalAmount);
        tempOrder.setSubtotal(subtotal);
        tempOrder.setShippingDiscountAmount(shippingDiscountAmount);

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

    public Integer confirmOrder(SubmitOrderRequestDTO requestDTO,String tempOrderId) throws SharedException {
        // 取出暫存訂單
        TemporaryOrder tempOrder = temporaryOrderStorage.getTemporaryOrder(tempOrderId);
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
        temporaryOrderStorage.confirmOrder(tempOrderId);

        return orderId;
    }

    public void cancelOrder(String tempOrderId) throws SharedException {
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
    }
}