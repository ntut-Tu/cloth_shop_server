package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.others.checkout.CheckoutBaseProductVariantModel;
import com.clothingstore.shop.dto.others.checkout.CheckoutBaseStoreOrderModel;
import com.clothingstore.shop.dto.others.discount.DiscountDetailsDTO;
import com.clothingstore.shop.dto.others.discount.SpecialDiscountDTO;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryOrder;
import com.clothingstore.shop.dto.request.checkout.ConfirmAmountRequestDTO;
import com.clothingstore.shop.dto.request.checkout.ConfirmDiscountRequestDTO;
import com.clothingstore.shop.dto.request.checkout.SubmitOrderRequestDTO;
import com.clothingstore.shop.enums.CouponType;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.repository.CheckoutRepository;
import com.clothingstore.shop.repository.DiscountRepository;
import com.clothingstore.shop.repository.InventoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final CheckoutRepository checkoutRepository;
    private final InventoryRepository inventoryRepository;
    private final JwtService jwtService;

    public DiscountService(DiscountRepository discountRepository, CheckoutService checkoutService, CheckoutRepository checkoutRepository, InventoryRepository inventoryRepository, JwtService jwtService) {
        this.discountRepository = discountRepository;
        this.checkoutRepository = checkoutRepository;
        this.inventoryRepository = inventoryRepository;
        this.jwtService = jwtService;
    }

    public TemporaryOrder calculateDiscounts(ConfirmAmountRequestDTO requestDTO, Integer customerId) throws SharedException {
        TemporaryOrder result = new TemporaryOrder(discountRepository);

        // 運費折扣
        if (requestDTO.getShipping_discount_code() != null) {
            DiscountDetailsDTO shippingDiscount = discountRepository.queryDiscountDetails(
                    discountRepository.queryDiscountIdByCode(requestDTO.getShipping_discount_code()),
                    CouponType.SHIPPING_DISCOUNT,
                    customerId
            );
            result.setShippingDiscountAmount(calculateShippingDiscount(shippingDiscount, checkoutRepository.queryShippingFee()));
        }

        // 商店訂單折扣
        for (CheckoutBaseStoreOrderModel storeOrder : requestDTO.getStore_orders()) {
            DiscountDetailsDTO storeDiscount = getStoreDiscount(storeOrder,customerId);
            result.addStoreDiscount(storeOrder.getStore_id(), storeDiscount);
        }

        return result;
    }

    public Integer calculateShippingDiscount(DiscountDetailsDTO discount, Integer shippingFee) {
        if (discount == null) return 0;
        if (discount.getRatio() != null) {
            return shippingFee * discount.getRatio() / 100;
        } else if (discount.getAmount() != null) {
            return discount.getAmount();
        }
        return 0;
    }
    public Integer calculateStoreDiscount(CheckoutBaseStoreOrderModel storeOrder,Integer customerId) throws SharedException {
        Integer storeDiscountAmount = 0;
        List<CheckoutBaseProductVariantModel> productVariants = storeOrder.getProduct_variants();
        // 获取特殊折扣
        if (storeOrder.getSpecial_discount_code() != null) {
            DiscountDetailsDTO storeDiscount = discountRepository.queryDiscountDetails(discountRepository.queryDiscountIdByCode(storeOrder.getSpecial_discount_code()), CouponType.SPECIAL_DISCOUNT,customerId);
            if (storeDiscount instanceof SpecialDiscountDTO) {
                // 提取特殊折扣相关信息
                Integer buyQuantity = ((SpecialDiscountDTO) storeDiscount).getBuyQuantity();
                Integer buyVariantId = ((SpecialDiscountDTO) storeDiscount).getBuyVariantId();
                Integer giftQuantity = ((SpecialDiscountDTO) storeDiscount).getGiftQuantity();
                Integer giftVariantId = ((SpecialDiscountDTO) storeDiscount).getGiftVariantId();

                // 检查是否符合买赠条件
                boolean hasBuyItem = false;
                boolean hasGiftItem = false;
                for (CheckoutBaseProductVariantModel productVariant : productVariants) {
                    if (productVariant.getProduct_variant_id() == buyVariantId && productVariant.getQuantity() >= buyQuantity) {
                        hasBuyItem = true;
                    }
                    if (productVariant.getProduct_variant_id() == giftVariantId && productVariant.getQuantity() >= giftQuantity) {
                        hasGiftItem = true;
                    }
                }

                // 如果满足购买条件但没有赠品，计算赠品价格
                if (hasBuyItem && (!hasGiftItem || productVariants.stream().noneMatch(pv -> pv.getProduct_variant_id() == giftVariantId && pv.getQuantity() >= giftQuantity))) {

                    Integer giftUnitPrice = inventoryRepository.getUnitPrice(giftVariantId);
                    if (giftUnitPrice != null) {
                        storeDiscountAmount += giftUnitPrice * giftQuantity;
                    }
                }
            }
        }

        // 获取季节性折扣
        if (storeOrder.getSeasonal_discount_code() != null) {
            DiscountDetailsDTO storeDiscount = discountRepository.queryDiscountDetails(discountRepository.queryDiscountIdByCode(storeOrder.getSeasonal_discount_code()), CouponType.SEASONAL_DISCOUNT,customerId);
            if (storeDiscount != null) {
                // 计算折扣金额（按比例或固定金额）
                Integer storeSubtotal = productVariants.stream()
                        .mapToInt(product -> {
                            Integer unitPrice = inventoryRepository.getUnitPrice(product.getProduct_variant_id());
                            return unitPrice != null ? unitPrice * product.getQuantity() : 0;
                        })
                        .sum();

                if (storeDiscount.getRatio() != null) {
                    storeDiscountAmount += storeSubtotal * storeDiscount.getRatio() / 100;
                } else if (storeDiscount.getAmount() != null) {
                    storeDiscountAmount += storeDiscount.getAmount();
                }
            }
        }

        return storeDiscountAmount;
    }

    public DiscountDetailsDTO getStoreDiscount(CheckoutBaseStoreOrderModel storeOrder,Integer customerId) throws SharedException {
        if (storeOrder.getSpecial_discount_code() != null) {
            return discountRepository.queryDiscountDetails(
                    discountRepository.queryDiscountIdByCode(storeOrder.getSpecial_discount_code()),
                    CouponType.SPECIAL_DISCOUNT,
                    customerId
            );
        } else if (storeOrder.getSeasonal_discount_code() != null) {
            return discountRepository.queryDiscountDetails(
                    discountRepository.queryDiscountIdByCode(storeOrder.getSeasonal_discount_code()),
                    CouponType.SEASONAL_DISCOUNT,
                    customerId
            );
        }
        return null;
    }
    public DiscountDetailsDTO getStoreDiscount(ConfirmDiscountRequestDTO requestDTO, Integer customerId) throws SharedException {
        Integer couponId = discountRepository.queryDiscountIdByCode(requestDTO.getDiscount_code());
        CouponType type = discountRepository.queryDiscountType(couponId);
        if (type.equals(CouponType.SPECIAL_DISCOUNT)) {
            return discountRepository.queryDiscountDetails(
                    discountRepository.queryDiscountIdByCode(requestDTO.getDiscount_code()),
                    CouponType.SPECIAL_DISCOUNT,
                    customerId
            );
        } else if (type.equals(CouponType.SEASONAL_DISCOUNT)) {
            return discountRepository.queryDiscountDetails(
                    discountRepository.queryDiscountIdByCode(requestDTO.getDiscount_code()),
                    CouponType.SEASONAL_DISCOUNT,
                    customerId
            );
        }
        return null;
    }

    public DiscountDetailsDTO getShippingDiscount(String shippingDiscountCode,Integer customerId) throws SharedException {
        return discountRepository.queryDiscountDetails(
                discountRepository.queryDiscountIdByCode(shippingDiscountCode),
                CouponType.SHIPPING_DISCOUNT,
                customerId
        );
    }

    public DiscountDetailsDTO getShippingDiscount(ConfirmDiscountRequestDTO requestDTO,Integer customerId) throws SharedException {
        Integer couponId = discountRepository.queryDiscountIdByCode(requestDTO.getDiscount_code());
        CouponType type = discountRepository.queryDiscountType(couponId);
        if (type.equals(CouponType.SHIPPING_DISCOUNT)) {
            return discountRepository.queryDiscountDetails(
                    discountRepository.queryDiscountIdByCode(requestDTO.getDiscount_code()),
                    CouponType.SHIPPING_DISCOUNT,
                    customerId
            );
        }
        return null;
    }

}

