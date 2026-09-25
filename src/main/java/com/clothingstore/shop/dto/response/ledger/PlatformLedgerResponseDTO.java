package com.clothingstore.shop.dto.response.ledger;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class PlatformLedgerResponseDTO {
    private Integer ledgerEntryId;
    private String ledgerType;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal totalBalance;
    private String transactionDate;
    private String notes;
    private Integer couponId;
    private Integer orderId;

    // Getters and Setters
    // Getters and Setters
    public Integer getLedgerEntryId() {
        return ledgerEntryId;
    }

    public void setLedgerEntryId(Integer ledgerEntryId) {
        this.ledgerEntryId = ledgerEntryId;
    }

    public String getLedgerType() {
        return ledgerType;
    }

    public void setLedgerType(String ledgerType) {
        this.ledgerType = ledgerType;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public void setTransactionDate(OffsetDateTime transactionDate) {
        this.transactionDate = transactionDate.toString();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getCouponId() {
        return couponId;
    }

    public void setCouponId(Integer couponId) {
        this.couponId = couponId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

}
