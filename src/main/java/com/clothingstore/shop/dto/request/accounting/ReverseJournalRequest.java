package com.clothingstore.shop.dto.request.accounting;

public record ReverseJournalRequest(String idempotencyKey, String reason) {
}
