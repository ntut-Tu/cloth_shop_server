package com.clothingstore.shop.manager;

import com.clothingstore.shop.dto.others.checkout.DiscountDetailsDTO;
import com.clothingstore.shop.dto.others.checkout.ProductVariantDTO;
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
    public DiscountDetailsDTO fetchStoreDiscountDetails(String discountCode) throws SharedException {
        // Fetch discount details from database
        try{
            Integer discountId = checkoutRepository.queryDiscountIdByCode(discountCode);
            Boolean isAvailable = checkoutRepository.queryDiscountIsAvailable(discountId);
            String discountType = checkoutRepository.queryDiscountType(discountId);
            DiscountDetailsDTO discountDetails = null;
            switch (discountType){
                case "special":
                    // Fetch special discount details
                    discountDetails = checkoutRepository.queryDiscountDetails(discountId);
                    break;
                case "shipping":
                    throw new SharedException("Invalid discount type");
                case "seasonal":
                    // Fetch seasonal discount details
                    discountDetails = checkoutRepository.queryDiscountDetails(discountId);
                    break;
                default:
                    throw new SharedException("Invalid discount type");
            }
            return discountDetails;
        }catch (SharedException e){
            throw e;
        }
    }
}
