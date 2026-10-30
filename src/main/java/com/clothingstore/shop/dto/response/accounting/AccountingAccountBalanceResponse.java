package com.clothingstore.shop.dto.response.accounting;

import java.math.BigDecimal;

public record AccountingAccountBalanceResponse(
        String accountCode,
        String accountName,
        String normalSide,
        BigDecimal balance
) {
}
