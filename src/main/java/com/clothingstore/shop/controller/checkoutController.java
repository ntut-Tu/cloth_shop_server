package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.checkout.ConfirmAmountRequestDTO;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.checkout.ConfirmAmountResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class checkoutController {
    @PostMapping("/confirm-amount")
    public ResponseEntity<ApiResponseDTO<ConfirmAmountResponseDTO>> confirmAmount(@RequestHeader("Authorization") String jwtToken, @RequestBody ConfirmAmountRequestDTO confirmAmountRequestDTO) {
        try{
            return null;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to confirm amount", null));
        }
    }
}
