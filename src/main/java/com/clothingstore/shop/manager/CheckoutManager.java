package com.clothingstore.shop.manager;

import com.clothingstore.shop.dto.others.discount.DiscountDetailsDTO;
import com.clothingstore.shop.dto.others.checkout.ProductVariantDTO;
import com.clothingstore.shop.dto.request.checkout.SubmitOrderRequestDTO;
import com.clothingstore.shop.enums.CouponType;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.repository.CheckoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutManager {
    @Autowired
    private CheckoutRepository checkoutRepository;
    public List<ProductVariantDTO> fetchProductDetails(List<Map.Entry<Integer, Integer>> productVariantIds) throws SharedException {
        List<ProductVariantDTO> productDetails = new ArrayList<>();
        for (Map.Entry<Integer, Integer> productVariantId : productVariantIds) {
            ProductVariantDTO productVariant = checkoutRepository.queryProductVariantById(productVariantId.getKey());
            if(productVariant != null && productVariant.getStockQuantity()>=productVariantId.getValue()){
                productVariant.setQuantity(productVariantId.getValue());
                if(productDetails == null) {
                    productDetails = List.of(productVariant);
                } else {
                    productDetails.add(productVariant);
                }
            }else{
                if(productVariant == null) {
                    throw new SharedException("Product not found");
                }else {
                    throw new SharedException("Out of stock");
                }
            }
        }
        return productDetails;
    }
    public DiscountDetailsDTO fetchStoreDiscountDetails(String discountCode,Integer customerId) throws SharedException {
        // Fetch discount details from database
        try{
            Integer discountId = checkoutRepository.queryDiscountIdByCode(discountCode);
            if(checkoutRepository.queryDiscountIsAvailable(discountId,customerId)){
                throw new SharedException("Discount not available");
            }
            CouponType discountType = checkoutRepository.queryDiscountType(discountId);
            DiscountDetailsDTO discountDetails = null;
            switch (discountType){
                case SPECIAL_DISCOUNT:
                    // Fetch special discount details
                    discountDetails = checkoutRepository.queryDiscountDetails(discountId, CouponType.SPECIAL_DISCOUNT);
                    break;
                case SEASONAL_DISCOUNT:
                    // Fetch seasonal discount details
                    discountDetails = checkoutRepository.queryDiscountDetails(discountId, CouponType.SEASONAL_DISCOUNT);
                    break;
                default:
                    throw new SharedException("Invalid discount type");
            }
            return discountDetails;
        }catch (SharedException e){
            throw e;
        }
    }
    public DiscountDetailsDTO fetchShippingDiscountDetails(String discountCode,Integer customerId) throws SharedException{
        try{
            Integer discountId = checkoutRepository.queryDiscountIdByCode(discountCode);
            if(checkoutRepository.queryDiscountIsAvailable(discountId,customerId)){
                throw new SharedException("Discount not available");
            }
            CouponType discountType = checkoutRepository.queryDiscountType(discountId);
            DiscountDetailsDTO discountDetails = null;
            switch (discountType){
                case SHIPPING_DISCOUNT:
                    // Fetch shipping discount details
                    discountDetails = checkoutRepository.queryDiscountDetails(discountId, CouponType.SHIPPING_DISCOUNT);
                    break;
                default:
                    throw new SharedException("Invalid discount type");
            }
            return discountDetails;
        }catch (SharedException e){
            throw e;
        }
    }
    public Integer storeOrder(SubmitOrderRequestDTO submitOrderRequestDTO) throws SharedException {
        // Save order details to database
//        return checkoutRepository.saveOrder(submitOrderRequestDTO);
        return 0;
    }
}
