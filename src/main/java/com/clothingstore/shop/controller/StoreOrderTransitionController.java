package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.orders.StoreOrderTransitionRequestDTO;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.orders.StoreOrderTransitionResponseDTO;
import com.clothingstore.shop.dto.response.orders.StoreOrderTransitionStateDTO;
import com.clothingstore.shop.exceptions.StoreOrderTransitionException;
import com.clothingstore.shop.service.StoreOrderTransitionService;
import com.clothingstore.shop.utils.TokenUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders/storeOrder/{storeOrderId}")
public class StoreOrderTransitionController {
    private final StoreOrderTransitionService service;

    public StoreOrderTransitionController(StoreOrderTransitionService service) {
        this.service = service;
    }

    @GetMapping("/transition-state")
    public ResponseEntity<ApiResponseDTO<StoreOrderTransitionStateDTO>> state(
            HttpServletRequest request, @PathVariable Integer storeOrderId) {
        try {
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Transition state retrieved",
                    service.getState(TokenUtils.extractTokenFromCookies(request), storeOrderId)));
        } catch (StoreOrderTransitionException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (JwtException e) {
            return error(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    @PostMapping("/transitions")
    public ResponseEntity<ApiResponseDTO<StoreOrderTransitionResponseDTO>> transition(
            HttpServletRequest request, @PathVariable Integer storeOrderId,
            @RequestBody(required = false) StoreOrderTransitionRequestDTO body) {
        try {
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Store order transitioned",
                    service.transition(TokenUtils.extractTokenFromCookies(request), storeOrderId, body)));
        } catch (StoreOrderTransitionException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (JwtException e) {
            return error(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    @GetMapping("/transitions")
    public ResponseEntity<ApiResponseDTO<List<StoreOrderTransitionResponseDTO>>> history(
            HttpServletRequest request, @PathVariable Integer storeOrderId,
            @RequestParam(required = false) Integer beforeTransitionId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Transition history retrieved",
                    service.getHistory(TokenUtils.extractTokenFromCookies(request), storeOrderId,
                            beforeTransitionId, limit)));
        } catch (StoreOrderTransitionException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (JwtException e) {
            return error(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    private <T> ResponseEntity<ApiResponseDTO<T>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiResponseDTO<>(false, message, null));
    }
}
