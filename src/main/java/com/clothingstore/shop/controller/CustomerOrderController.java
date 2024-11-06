package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.repository.orders.OrderItemDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.OrderSummaryRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.StoreOrderSummaryRepositoryDTO;
import com.clothingstore.shop.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/orders")
public class CustomerOrderController {

    private final OrderService orderService;

    @Autowired
    public CustomerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 取得使用者訂單簡介列表
    @GetMapping("/user")
    public ResponseEntity<ApiResponseDTO<List<OrderSummaryRepositoryDTO>>> getUserOrderSummaries(
            @RequestHeader("Authorization") String jwtToken,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        String token = jwtToken.replace("Bearer ", "");
        List<OrderSummaryRepositoryDTO> orderSummaries = orderService.getUserOrderSummaries(token, page, size);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Order summaries retrieved successfully", orderSummaries));
    }

    // 取得特定訂單的商家訂單列表
    @GetMapping("/{orderId}/stores")
    public ResponseEntity<ApiResponseDTO<List<StoreOrderSummaryRepositoryDTO>>> getStoreOrdersByOrderId(
            @RequestHeader("Authorization") String jwtToken,
            @PathVariable Integer orderId) {
        String token = jwtToken.replace("Bearer ", "");
        List<StoreOrderSummaryRepositoryDTO> storeOrders = orderService.getStoreOrdersByOrderId(token, orderId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Store orders retrieved successfully", storeOrders));
    }

    // 取得商家訂單的商品詳情
    @GetMapping("/stores/{storeOrderId}/items")
    public ResponseEntity<ApiResponseDTO<List<OrderItemDetailRepositoryDTO>>> getOrderItemsByStoreOrderId(
            @RequestHeader("Authorization") String jwtToken,
            @PathVariable Integer storeOrderId) {
        String token = jwtToken.replace("Bearer ", "");
        List<OrderItemDetailRepositoryDTO> orderItems = orderService.getOrderItemsByStoreOrderId(token, storeOrderId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Order items retrieved successfully", orderItems));
    }
}
