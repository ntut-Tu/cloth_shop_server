package com.clothingstore.shop.manager;

import com.clothingstore.shop.dto.others.checkout.DiscountDetailsDTO;
import com.clothingstore.shop.dto.others.checkout.ProductVariantDTO;
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
public List<ProductVariantDTO> fetchProductDetails(List<Map.Entry<Integer, Integer>> productVariantIds) {
    // 無存貨或存貨不足邏輯尚未確定
    List<ProductVariantDTO> productDetails = new ArrayList<>();
    for (Map.Entry<Integer, Integer> productVariantId : productVariantIds) {
        ProductVariantDTO productVariant = checkoutRepository.queryProductVariantById(productVariantId.getKey());
            if(productVariant != null && productVariant.getStockQuantity()>=productVariantId.getValue()){ {
                productVariant.setQuantity(productVariantId.getValue());
                if(productDetails == null) {
                    productDetails = List.of(productVariant);
                } else {
                    productDetails.add(productVariant);
                }
            }
        }
    }
    return productDetails;
}
public DiscountDetailsDTO fetchDiscountDetails(Integer discountId) {
    // Fetch discount details from database

    return null;
}
