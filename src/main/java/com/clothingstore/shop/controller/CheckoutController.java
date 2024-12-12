package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.checkout.ConfirmAmountRequestDTO;
import com.clothingstore.shop.dto.request.checkout.ConfirmDiscountRequestDTO;
import com.clothingstore.shop.dto.request.checkout.SubmitOrderRequestDTO;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.checkout.ConfirmAmountResponseDTO;
import com.clothingstore.shop.dto.response.checkout.SubmitOrderResponseDTO;
import com.clothingstore.shop.service.CheckoutService;
import com.clothingstore.shop.service.DiscountService;
import com.clothingstore.shop.service.JwtService;
import com.clothingstore.shop.utils.TokenUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final DiscountService discountService;
    private final JwtService jwtService;
    private final CheckoutService checkoutService;

    public CheckoutController(DiscountService discountService, JwtService jwtService, CheckoutService checkoutService) {
        this.discountService = discountService;
        this.jwtService = jwtService;
        this.checkoutService = checkoutService;
    }

    @PostMapping("/confirm-amount")
    public ResponseEntity<ApiResponseDTO<ConfirmAmountResponseDTO>> confirmAmount(
            HttpServletRequest request,
            @RequestBody ConfirmAmountRequestDTO confirmAmountRequestDTO) {
        try {
            // 從 cookie 中提取 token
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }

            // TODO: 處理確認金額的業務邏輯
            // 返回處理結果（此處用 null 作為示例）
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Amount confirmed successfully", checkoutService.saveTemporaryOrder(confirmAmountRequestDTO, token)));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }
    @PostMapping("/confirm-discount")
    public ResponseEntity<ApiResponseDTO<ConfirmDiscountRequestDTO>> confirmDiscount(
            HttpServletRequest request,
            @RequestBody ConfirmDiscountRequestDTO confirmDiscountRequestDTO) {
        try {
            // 從 cookie 中提取 token
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            Integer customerId = jwtService.extractUserId(token);
            String type = confirmDiscountRequestDTO.getType();
            if(type.equals("order")){
                ResponseEntity.ok(new ApiResponseDTO<>(true, "Amount discount successfully", discountService.getStoreDiscount(confirmDiscountRequestDTO,customerId)));
            }else if(type.equals("store_order")){
                ResponseEntity.ok(new ApiResponseDTO<>(true, "Amount discount successfully", discountService.getShippingDiscount(confirmDiscountRequestDTO,customerId)));
            }
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Amount discount successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }
    @PostMapping("submit-order")
    public ResponseEntity<ApiResponseDTO<SubmitOrderResponseDTO>> submitOrder(
            HttpServletRequest request,
            @RequestBody SubmitOrderRequestDTO submitOrderRequestDTO) {
        try {
            // 從 cookie 中提取 token
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Amount discount successfully", checkoutService.confirmOrder(submitOrderRequestDTO, token)));
        }catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }
    @PostMapping("cancel-order")
    public ResponseEntity<ApiResponseDTO<String>> cancelOrder(
            HttpServletRequest request,
            @RequestBody String orderId) {
        try {
            // 從 cookie 中提取 token
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Order canceled successfully", checkoutService.cancelOrder(orderId)));
        }catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponseDTO<>(false, e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponseDTO<>(false, e.getMessage(), null));
            }
    }
}
