package com.clothingstore.shop.accounting;

import com.clothingstore.shop.dto.response.accounting.AccountingReconciliationResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountingReconciliationCalculatorTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

    @Test
    void balancedPaidOrderReconcilesExactly() {
        AccountingReconciliationResponse result = calculate(
                snapshot(1, "PAID", 100, 10L, 2, 100, 100, null, 0, 0, 0));
        assertTrue(result.reconciled());
        assertEquals(BigDecimal.valueOf(100), result.expectedPaidAmount());
        assertEquals(BigDecimal.valueOf(100), result.netPostedAmount());
    }

    @Test
    void missingPaymentPostingIsReportedByOrder() {
        AccountingReconciliationResponse result = calculate(
                snapshot(2, "PAID", 250, null, 0, 0, 0, null, 0, 0, 0));
        assertFalse(result.reconciled());
        assertEquals(List.of(2), result.missingPaymentOrderIds());
        assertEquals(BigDecimal.ZERO, result.netPostedAmount());
    }

    @Test
    void amountDriftIsReportedEvenIfJournalItselfIsBalanced() {
        AccountingReconciliationResponse result = calculate(
                snapshot(3, "PAID", 250, 30L, 2, 200, 200, null, 0, 0, 0));
        assertFalse(result.reconciled());
        assertEquals(List.of(3), result.amountMismatchOrderIds());
    }

    @Test
    void reversalOfStillPaidOrderRequiresInvestigation() {
        AccountingReconciliationResponse result = calculate(
                snapshot(4, "PAID", 100, 40L, 2, 100, 100, 41L, 2, 100, 100));
        assertFalse(result.reconciled());
        assertEquals(List.of(4), result.reversedPaidOrderIds());
        assertEquals(BigDecimal.ZERO, result.netPostedAmount());
    }

    @Test
    void fullyReversedInactiveOrderReconciles() {
        AccountingReconciliationResponse result = calculate(
                snapshot(5, "CANCELLED", 100, 50L, 2, 100, 100, 51L, 2, 100, 100));
        assertTrue(result.reconciled());
        assertEquals(BigDecimal.ZERO, result.expectedPaidAmount());
        assertEquals(BigDecimal.ZERO, result.netPostedAmount());
    }

    @Test
    void inactiveOrderWithUnreversedPaymentIsReported() {
        AccountingReconciliationResponse result = calculate(
                snapshot(6, "CANCELLED", 100, 60L, 2, 100, 100, null, 0, 0, 0));
        assertFalse(result.reconciled());
        assertEquals(List.of(6), result.unreversedInactiveOrderIds());
    }

    @Test
    void unbalancedOrIncompleteJournalIsReported() {
        AccountingReconciliationResponse result = calculate(
                snapshot(7, "PAID", 100, 70L, 1, 100, 90, null, 0, 0, 0));
        assertFalse(result.reconciled());
        assertEquals(List.of(70L), result.invalidJournalIds());
    }

    private AccountingReconciliationResponse calculate(AccountingOrderSnapshot snapshot) {
        return AccountingReconciliationCalculator.calculate(DAY, List.of(snapshot));
    }

    private AccountingOrderSnapshot snapshot(int orderId, String status, Integer total,
            Long paymentId, long paymentLines, long paymentDebits, long paymentCredits,
            Long reversalId, long reversalLines, long reversalDebits, long reversalCredits) {
        return new AccountingOrderSnapshot(orderId, status, total, paymentId, paymentLines,
                BigDecimal.valueOf(paymentDebits), BigDecimal.valueOf(paymentCredits),
                reversalId, reversalLines, BigDecimal.valueOf(reversalDebits),
                BigDecimal.valueOf(reversalCredits));
    }
}
