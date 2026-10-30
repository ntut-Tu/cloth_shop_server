package com.clothingstore.shop.promotion;

import java.util.List;

public record PromotionCart(List<Line> lines, int previousOrderCount) {
    public record Line(int variantId, int vendorId, String category, int quantity, int unitPrice) {
        public long subtotal() {
            return (long) quantity * unitPrice;
        }
    }

    public long subtotal() {
        return lines.stream().mapToLong(Line::subtotal).sum();
    }
}
