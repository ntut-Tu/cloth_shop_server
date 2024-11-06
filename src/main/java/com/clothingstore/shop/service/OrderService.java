package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.repository.orders.OrderItemDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.OrderSummaryRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.StoreOrderSummaryRepositoryDTO;
import com.clothingstore.shop.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final JwtService jwtService;

    @Autowired
    public OrderService(OrderRepository orderRepository, JwtService jwtService) {
        this.orderRepository = orderRepository;
        this.jwtService = jwtService;
    }

    // 取得使用者訂單列表（訂單簡介）
    public List<OrderSummaryRepositoryDTO> getUserOrderSummaries(String jwtToken, int page, int size) {
        Integer userId = jwtService.extractUserId(jwtToken);
        int offset = (page - 1) * size;
        return orderRepository.findOrderSummariesByCustomerId(userId, size, offset);
    }

    // 取得特定訂單的商家訂單列表
    public List<StoreOrderSummaryRepositoryDTO> getStoreOrdersByOrderId(String jwtToken, Integer orderId) {
        Integer userId = jwtService.extractUserId(jwtToken);
        // 可以加入額外驗證以確認此訂單屬於當前使用者
        return orderRepository.findStoreOrdersByOrderId(orderId);
    }

    // 取得商家訂單的商品詳情
    public List<OrderItemDetailRepositoryDTO> getOrderItemsByStoreOrderId(String jwtToken, Integer storeOrderId) {
        Integer userId = jwtService.extractUserId(jwtToken);
        // 可以加入額外驗證以確認此商家訂單屬於當前使用者
        return orderRepository.findOrderItemsByStoreOrderId(storeOrderId);
    }

}
