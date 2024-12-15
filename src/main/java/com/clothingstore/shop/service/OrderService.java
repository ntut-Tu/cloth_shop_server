package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.repository.orders.OrderDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.OrderSummaryRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.StoreOrderSummaryRepositoryDTO;
import com.clothingstore.shop.dto.response.vendorOrder.VendorOrderResponseDTO;
import com.clothingstore.shop.exceptions.SharedException;
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
    public List<OrderSummaryRepositoryDTO> getUserOrderSummaries(String jwtToken, int page, int size) throws SharedException {
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

    public void updateOrderStatus(String token, Integer orderId, String status) throws SharedException {
        // 顧客可以完成但管理員及商家只能退款
        Integer userId = jwtService.extractUserId(token);
        String role = jwtService.extractRole(token);
        if (role.equals("customer")) {
            if(!orderRepository.isOrderBelongToCustomer(userId, orderId)) {
                throw new IllegalArgumentException("Order does not belong to customer");
            }
            if(!orderRepository.isEditOrderStatusValid(role,orderId)) {
                throw new IllegalArgumentException("Invalid order status");
            }
            orderRepository.updateOrderStatus(orderId, status);
        } else {
            throw new IllegalArgumentException("Invalid role cannot update order status");
        }
    }

    public List<OrderDetailRepositoryDTO> getOrderDetailsByOrderId(String token, Integer storeOrderId) {
        Integer userId = jwtService.extractUserId(token);
        if(!orderRepository.isOrderBelongToCustomer(userId, storeOrderId)) {
            throw new IllegalArgumentException("Order does not belong to customer");
        }
        return orderRepository.findOrderDetailsByOrderId(storeOrderId);
    }

    public void updateStoreOrderStatus(String token, Integer storeOrderId, String status) throws SharedException {
        Integer userId = jwtService.extractUserId(token);
        if(!orderRepository.isStoreOrderBelongToVendor(userId, storeOrderId)) {
            throw new IllegalArgumentException("Order does not belong to vendor");
        }
        orderRepository.updateStoreOrderStatus(storeOrderId, status);
    }

    public List<VendorOrderResponseDTO> getVendorStoreOrders(String token, int page, int size) throws SharedException {
        Integer userId = jwtService.extractUserId(token);
        int offset = (page - 1) * size;
        return orderRepository.findStoreOrderSummariesByVendorId(userId, size, offset);
    }
}
