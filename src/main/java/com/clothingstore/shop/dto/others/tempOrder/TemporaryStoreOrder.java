package com.clothingstore.shop.dto.others.tempOrder;

import com.clothingstore.shop.dto.others.discount.DiscountDetailsDTO;
import com.clothingstore.shop.dto.others.discount.SpecialDiscountDTO;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.repository.DiscountRepository;

import java.util.ArrayList;

public class TemporaryStoreOrder {
    private final DiscountRepository discountRepository;

    private Integer storeId;
    private ArrayList<TemporaryProductVariant> productVariants;
    private Integer subtotal;
    private Integer discountAmount;
    private DiscountDetailsDTO discountDetails; // 折扣详情

    public TemporaryStoreOrder(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    // Getters and Setters
    public Integer getStoreId() {
        return storeId;
    }

    public void setStoreId(Integer storeId) {
        this.storeId = storeId;
    }

    public ArrayList<TemporaryProductVariant> getProductVariants() {
        return productVariants;
    }

    public void setProductVariants(ArrayList<TemporaryProductVariant> productVariants) {
        this.productVariants = productVariants;
    }

    public Integer getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Integer subtotal) {
        this.subtotal = subtotal;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void setDiscountDetails(DiscountDetailsDTO storeDiscount) {
        this.discountDetails = storeDiscount;
    }
    public DiscountDetailsDTO getDiscountDetails() {
        return discountDetails;
    }

    public Integer getSpecialDiscountId() throws SharedException {
        if(discountDetails instanceof SpecialDiscountDTO) {
            return discountRepository.queryDiscountIdByCode(discountDetails.getCode());
        } else {
            return null;
        }
    }

    public Integer getSeasonalDiscountId() throws SharedException {
        if(discountDetails instanceof SpecialDiscountDTO) {
            return discountRepository.queryDiscountIdByCode(discountDetails.getCode());
        } else {
            return null;
        }
    }
}
