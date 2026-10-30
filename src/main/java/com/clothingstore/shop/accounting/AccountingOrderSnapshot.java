package com.clothingstore.shop.accounting;

import java.math.BigDecimal;

/** One order and its immutable payment/reversal posting totals. */
public record AccountingOrderSnapshot(
        int orderId,
        String payStatus,
        Integer totalAmount,
        Long paymentJournalId,
        long paymentLineCount,
        BigDecimal paymentDebits,
        BigDecimal paymentCredits,
        Long reversalJournalId,
        long reversalLineCount,
        BigDecimal reversalDebits,
        BigDecimal reversalCredits
) {
}
