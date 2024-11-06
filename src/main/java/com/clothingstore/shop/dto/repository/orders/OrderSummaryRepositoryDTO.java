package com.clothingstore.shop.dto.repository.orders;

import java.sql.Timestamp;

public class OrderSummaryRepositoryDTO {
    private Integer orderId;
    private Timestamp orderDate;
    private Integer totalAmount;
    private String payStatus;
    private String shipStatus;
    // Getters and Setters
}
