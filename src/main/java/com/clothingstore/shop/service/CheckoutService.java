package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.others.checkout.*;
import com.clothingstore.shop.dto.others.discount.DiscountDetailsDTO;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryOrder;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryProductVariant;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryStoreOrder;
import com.clothingstore.shop.dto.request.checkout.ConfirmAmountRequestDTO;
import com.clothingstore.shop.dto.request.checkout.SubmitOrderRequestDTO;
import com.clothingstore.shop.dto.response.checkout.ConfirmAmountResponseDTO;
import com.clothingstore.shop.dto.response.checkout.SubmitOrderResponseDTO;
import com.clothingstore.shop.enums.PayMethod;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.repository.CheckoutRepository;
import com.clothingstore.shop.repository.CheckoutReservationRepository;
import com.clothingstore.shop.repository.DiscountRepository;
import com.clothingstore.shop.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Service
public class CheckoutService {
    private final JwtService jwtService;
    private final CheckoutRepository checkoutRepository;
    private final CheckoutReservationRepository reservations;
    private final InventoryRepository inventoryRepository;
    private final DiscountService discountService;
    private final DiscountRepository discountRepository;

    @Autowired
    public CheckoutService(JwtService jwtService, CheckoutRepository checkoutRepository, CheckoutReservationRepository reservations, InventoryRepository inventoryRepository, @Lazy DiscountService discountService, DiscountRepository discountRepository) {
        this.jwtService = jwtService;
        this.checkoutRepository = checkoutRepository;
        this.reservations = reservations;
        this.inventoryRepository = inventoryRepository;
        this.discountService = discountService;
        this.discountRepository = discountRepository;
    }
    public ConfirmAmountResponseDTO saveTemporaryOrder(ConfirmAmountRequestDTO requestDTO, String jwtToken) throws SharedException {
        Integer customerId = jwtService.extractUserId(jwtToken);
        // 驗證訂單
        validateOrder(requestDTO);

        // Compute the quote before holding stock; a failed quote cannot leak a reservation.
        TemporaryOrder tempOrder;
        try {
            tempOrder = calculateTemporaryOrder(requestDTO, customerId);
        } catch (ArithmeticException e) {
            throw new SharedException("Order amount exceeds the supported range", e);
        }
        String reservationId = reservations.reserve(ReservationSnapshot.from(tempOrder));
        return new ConfirmAmountResponseDTO(tempOrder.getSubtotal(), tempOrder.getShippingFee(),
                tempOrder.getShippingDiscountAmount() + tempOrder.getTotalStoreDiscountAmount(),
                tempOrder.getTotalAmount(), reservationId);
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
        int shippingFee = checkoutRepository.queryShippingFee();
        Integer shippingDiscountAmount = discountService.calculateShippingDiscount(shippingDiscount, shippingFee);
        if (shippingDiscountAmount < 0 || shippingDiscountAmount > shippingFee) {
            throw new SharedException("Shipping discount exceeds the shipping fee");
        }
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
                Integer unitPrice = inventoryRepository.getUnitPriceForStore(
                        productVariant.getProduct_variant_id(), storeOrder.getStore_id());
                Integer productTotal = Math.multiplyExact(unitPrice, productVariant.getQuantity());

                // 累加商店金額
                storeSubtotal = Math.addExact(storeSubtotal, productTotal);

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
            if (storeDiscountAmount < 0 || storeDiscountAmount > storeSubtotal) {
                throw new SharedException("Store discount exceeds the store subtotal");
            }

            // 設定商店訂單金額
            tempStoreOrder.setSubtotal(storeSubtotal);
            tempStoreOrder.setDiscountAmount(storeDiscountAmount);
            tempStoreOrder.setTotalAmount(storeSubtotal - storeDiscountAmount);
            totalStoreDiscount = Math.addExact(totalStoreDiscount, storeDiscountAmount);

            // 累加訂單總金額
            subtotal = Math.addExact(subtotal, storeSubtotal);
            totalAmount = Math.addExact(totalAmount, storeSubtotal - storeDiscountAmount);

            // 加入臨時訂單
            tempOrder.getStoreOrders().add(tempStoreOrder);
        }

        // 更新訂單
        totalAmount = Math.addExact(totalAmount, shippingFee);
        totalAmount = Math.subtractExact(totalAmount, shippingDiscountAmount);
        tempOrder.setTotalAmount(totalAmount);
        tempOrder.setSubtotal(subtotal);
        tempOrder.setShippingDiscountAmount(shippingDiscountAmount);
        if(shippingDiscount != null){
            tempOrder.setShippingDiscountCode(shippingDiscount.getCode());
        }
        tempOrder.setShippingFee(shippingFee-shippingDiscountAmount);
        tempOrder.setTotalStoreDiscountAmount(totalStoreDiscount);
        return tempOrder;
    }


    public void validateOrder(ConfirmAmountRequestDTO requestDTO) throws SharedException {
        // 檢查訂單是否有商品
        if (requestDTO == null || requestDTO.getStore_orders() == null || requestDTO.getStore_orders().isEmpty()) {
            throw new SharedException("Order is empty");
        }

        Set<Integer> variantIds = new HashSet<>();
        for (CheckoutBaseStoreOrderModel storeOrder : requestDTO.getStore_orders()) {
            if (storeOrder == null || storeOrder.getStore_id() <= 0 ||
                    storeOrder.getProduct_variants() == null || storeOrder.getProduct_variants().isEmpty()) {
                throw new SharedException("Order is empty");
            }
            for (CheckoutBaseProductVariantModel variant : storeOrder.getProduct_variants()) {
                if (variant == null || variant.getProduct_variant_id() <= 0 || variant.getQuantity() <= 0) {
                    throw new SharedException("Product variant and quantity must be positive");
                }
                if (!variantIds.add(variant.getProduct_variant_id())) {
                    throw new SharedException("Duplicate product variant in order");
                }
            }
        }
    }

    public SubmitOrderResponseDTO confirmOrder(SubmitOrderRequestDTO requestDTO, String jwtToken) throws SharedException {
        if (requestDTO == null || requestDTO.getOrder_id() == null ||
                requestDTO.getOrder_id().isBlank() || requestDTO.getPayment_method() == null ||
                requestDTO.getDelivery_type() == null || requestDTO.getDelivery_type().isBlank()) {
            throw new SharedException("Reservation ID, payment method, and delivery type are required");
        }
        if (!PayMethod.CARD.getMethod().equals(requestDTO.getPayment_method()) &&
                !PayMethod.CASH.getMethod().equals(requestDTO.getPayment_method())) {
            throw new SharedException("Unsupported payment method");
        }
        int userId = jwtService.extractUserId(jwtToken);
        CheckoutReservationRepository.Confirmation confirmation =
                reservations.confirm(requestDTO.getOrder_id(), userId, requestDTO);
        ReservationSnapshot quote = confirmation.quote();
        return new SubmitOrderResponseDTO(confirmation.orderId(), confirmation.paymentStatus(),
                quote.totalAmount(), quote.totalStoreDiscountAmount() + quote.shippingDiscountAmount(),
                quote.totalAmount(), null);
    }

    public String cancelOrder(String tempOrderId, String jwtToken) throws SharedException {
        if (tempOrderId == null || tempOrderId.isBlank()) {
            throw new SharedException("Reservation ID is required");
        }
        reservations.cancel(tempOrderId, jwtService.extractUserId(jwtToken));
        return "Order canceled successfully";
    }
}
