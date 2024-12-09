package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.repository.orders.OrderDetailRepositoryDTO;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.repository.orders.OrderSummaryRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.StoreOrderSummaryRepositoryDTO;
import com.clothingstore.shop.service.OrderService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 從 cookie 中取得使用者的訂單簡介列表
     * @param request - HttpServletRequest 用於從 cookie 中取得 token
     * @param page - 分頁的頁碼
     * @param size - 每頁的訂單數量
     * @return 包含訂單摘要的 ApiResponseDTO
     */
    @GetMapping("/user")
    public ResponseEntity<ApiResponseDTO<List<OrderSummaryRepositoryDTO>>> getUserOrderSummaries(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        String token = TokenUtils.extractTokenFromCookies(request);
        List<OrderSummaryRepositoryDTO> orderSummaries = orderService.getUserOrderSummaries(token, page, size);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Order summaries retrieved successfully", orderSummaries));
    }

    /**
     * 根據訂單 ID 從 cookie 中取得商家訂單列表
     * @param request - HttpServletRequest 用於從 cookie 中取得 token
     * @param orderId - 訂單的唯一標識符
     * @return 包含商家訂單摘要的 ApiResponseDTO
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponseDTO<List<StoreOrderSummaryRepositoryDTO>>> getStoreOrdersByOrderId(
            HttpServletRequest request,
            @PathVariable Integer orderId) {
        String token = TokenUtils.extractTokenFromCookies(request);
        List<StoreOrderSummaryRepositoryDTO> storeOrders = orderService.getStoreOrdersByOrderId(token, orderId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Store orders retrieved successfully", storeOrders));
    }


    /**
     * 根據商家訂單 ID 從 cookie 中取得商品詳情
     * @param request - HttpServletRequest 用於從 cookie 中取得 token
     * @param orderId - 商家訂單的唯一標識符
     * @return 包含商品詳情的 ApiResponseDTO
     */
    @GetMapping("/{storeOrderId}/items")
    public ResponseEntity<ApiResponseDTO<List<OrderDetailRepositoryDTO>>> getOrderDetailsByStoreOrderId(
            HttpServletRequest request,
            @PathVariable Integer orderId) {
        String token = TokenUtils.extractTokenFromCookies(request);
        List<OrderDetailRepositoryDTO> orderDetails = orderService.getOrderDetailsByOrderId(token, orderId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Order details retrieved successfully", orderDetails));
    }

    @PostMapping("/{role}/{orderId}/status")
    public ResponseEntity<ApiResponseDTO<String>> updateOrderStatus(
            HttpServletRequest request,
            @PathVariable String role,
            @PathVariable Integer orderId,
            @RequestParam String status) {
        String token = TokenUtils.extractTokenFromCookies(request);
        orderService.updateOrderStatus(token, role, orderId, status);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Order status updated successfully", null));
    }

}
