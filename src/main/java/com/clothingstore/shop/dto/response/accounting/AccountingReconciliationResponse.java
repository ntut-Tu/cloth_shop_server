package com.clothingstore.shop.dto.response.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AccountingReconciliationResponse(
        LocalDate orderDate,
        int orderCount,
        BigDecimal expectedPaidAmount,
        BigDecimal netPostedAmount,
        List<Integer> missingPaymentOrderIds,
        List<Integer> amountMismatchOrderIds,
        List<Integer> reversedPaidOrderIds,
        List<Integer> unreversedInactiveOrderIds,
        List<Integer> invalidOrderIds,
        List<Long> invalidJournalIds,
        boolean reconciled
) {
}
