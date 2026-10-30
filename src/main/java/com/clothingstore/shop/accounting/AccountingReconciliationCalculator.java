package com.clothingstore.shop.accounting;

import com.clothingstore.shop.dto.response.accounting.AccountingReconciliationResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class AccountingReconciliationCalculator {
    private AccountingReconciliationCalculator() {
    }

    public static AccountingReconciliationResponse calculate(
            LocalDate orderDate, List<AccountingOrderSnapshot> orders) {
        BigDecimal expected = BigDecimal.ZERO;
        BigDecimal actual = BigDecimal.ZERO;
        List<Integer> missing = new ArrayList<>();
        List<Integer> mismatched = new ArrayList<>();
        List<Integer> reversedPaid = new ArrayList<>();
        List<Integer> unreversedInactive = new ArrayList<>();
        List<Integer> invalidOrders = new ArrayList<>();
        List<Long> invalidJournals = new ArrayList<>();
        for (AccountingOrderSnapshot order : orders) {
            BigDecimal amount = order.totalAmount() == null
                    ? BigDecimal.ZERO : BigDecimal.valueOf(order.totalAmount());
            boolean paid = "PAID".equalsIgnoreCase(order.payStatus());
            if (paid) {
                expected = expected.add(amount);
                if (order.totalAmount() == null || order.totalAmount() < 0) {
                    invalidOrders.add(order.orderId());
                }
                if (amount.signum() > 0 && order.paymentJournalId() == null) {
                    missing.add(order.orderId());
                }
                if (order.reversalJournalId() != null) {
                    reversedPaid.add(order.orderId());
                }
                if (order.paymentJournalId() != null
                        && (order.paymentDebits().compareTo(amount) != 0
                        || order.paymentCredits().compareTo(amount) != 0)) {
                    mismatched.add(order.orderId());
                }
            } else if (order.paymentJournalId() != null && order.reversalJournalId() == null) {
                unreversedInactive.add(order.orderId());
            }
            actual = actual.add(order.paymentDebits()).subtract(order.reversalCredits());
            if (order.paymentJournalId() != null
                    && (order.paymentLineCount() < 2
                    || order.paymentDebits().signum() <= 0
                    || order.paymentDebits().compareTo(order.paymentCredits()) != 0)) {
                invalidJournals.add(order.paymentJournalId());
            }
            if (order.reversalJournalId() != null
                    && (order.reversalLineCount() < 2
                    || order.reversalDebits().compareTo(order.reversalCredits()) != 0
                    || order.reversalDebits().compareTo(order.paymentCredits()) != 0)) {
                invalidJournals.add(order.reversalJournalId());
            }
        }
        boolean reconciled = expected.compareTo(actual) == 0 && missing.isEmpty()
                && mismatched.isEmpty() && reversedPaid.isEmpty()
                && unreversedInactive.isEmpty() && invalidOrders.isEmpty()
                && invalidJournals.isEmpty();
        return new AccountingReconciliationResponse(orderDate, orders.size(), expected, actual,
                missing, mismatched, reversedPaid, unreversedInactive, invalidOrders,
                invalidJournals, reconciled);
    }
}
