package com.clothingstore.shop.dto.response.accounting;

import java.math.BigDecimal;

public record AccountingPostingResponse(
        Long postingId,
        String accountCode,
        BigDecimal debit,
        BigDecimal credit
) {
}
