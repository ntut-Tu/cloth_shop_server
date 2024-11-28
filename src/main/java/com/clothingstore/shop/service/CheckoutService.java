package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.checkout.ConfirmAmountRequestDTO;
import com.clothingstore.shop.dto.request.checkout.ConfirmDiscountRequestDTO;
import com.clothingstore.shop.dto.request.checkout.SubmitOrderRequestDTO;
import com.clothingstore.shop.dto.response.checkout.ConfirmAmountResponseDTO;
import com.clothingstore.shop.dto.response.checkout.SubmitOrderResponseDTO;
import com.clothingstore.shop.repository.CheckoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {
    private final JwtService jwtService;
    private final CheckoutRepository checkoutRepository;

    @Autowired
    public CheckoutService(JwtService jwtService, CheckoutRepository checkoutRepository) {
        this.jwtService = jwtService;
        this.checkoutRepository = checkoutRepository;
    }
}

//    public ConfirmAmountResponseDTO confirmAmount(String jwtToken, ConfirmAmountRequestDTO confirmAmountRequestDTO) {
//        Integer userId = jwtService.extractUserId(jwtToken);
//        //從request中提取每個商品從repository中取得的價格加總並扣掉優惠金額等完成confirmAmountResponseDTO的處理及建立
//    }
//
//    public ConfirmAmountResponseDTO confirmDiscount(String jwtToken, ConfirmDiscountRequestDTO confirmDiscountRequestDTO) {
//        Integer userId = jwtService.extractUserId(jwtToken);
//        //處裡並完成confirmDiscountResponseDTO的建立
//    }
//
//    public SubmitOrderResponseDTO submitOrder(String jwtToken, SubmitOrderRequestDTO submitOrderRequestDTO) {
//        Integer userId = jwtService.extractUserId(jwtToken);
//        //處裡並完成submitOrderResponseDTO的建立
//    }
//}