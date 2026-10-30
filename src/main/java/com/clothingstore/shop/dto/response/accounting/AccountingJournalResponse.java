package com.clothingstore.shop.dto.response.accounting;

import java.time.OffsetDateTime;
import java.util.List;

public record AccountingJournalResponse(
        Long journalId,
        Integer orderId,
        String entryType,
        String eventKey,
        OffsetDateTime occurredAt,
        Long reversesJournalId,
        Integer actorUserId,
        String reason,
        List<AccountingPostingResponse> postings
) {
}
