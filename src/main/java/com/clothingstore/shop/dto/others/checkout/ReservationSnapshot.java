package com.clothingstore.shop.dto.others.checkout;

import com.clothingstore.shop.dto.others.tempOrder.TemporaryOrder;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryProductVariant;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryStoreOrder;
import com.clothingstore.shop.exceptions.SharedException;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

/** The prices and discount identifiers accepted by the customer at reservation time. */
public record ReservationSnapshot(
        int userId,
        int subtotal,
        int shippingFee,
        int shippingDiscountAmount,
        int totalStoreDiscountAmount,
        int totalAmount,
        Integer shippingDiscountId,
        List<StoreQuote> stores) {

    public ReservationSnapshot {
        stores = List.copyOf(stores);
    }

    public static ReservationSnapshot from(TemporaryOrder order) throws SharedException {
        List<StoreQuote> stores = new ArrayList<>();
        for (TemporaryStoreOrder store : order.getStoreOrders()) {
            stores.add(new StoreQuote(store.getStoreId(), store.getSubtotal(),
                    store.getDiscountAmount(), store.getTotalAmount(),
                    store.getSpecialDiscountId(), store.getSeasonalDiscountId(),
                    store.getProductVariants().stream().map(ReservationSnapshot::item).toList()));
        }
        return new ReservationSnapshot(order.getCustomerId(), order.getSubtotal(),
                order.getShippingFee(), order.getShippingDiscountAmount(),
                order.getTotalStoreDiscountAmount(), order.getTotalAmount(),
                order.getShippingDiscountId(), stores);
    }

    private static ItemQuote item(TemporaryProductVariant item) {
        return new ItemQuote(item.getProductVariantId(), item.getQuantity(),
                item.getUnitPrice(), item.getTotalAmount());
    }

    /** Sorting prevents opposite item orders from deadlocking on variant rows. */
    public Map<Integer, Integer> quantitiesByVariant() {
        Map<Integer, Integer> quantities = new TreeMap<>();
        for (StoreQuote store : stores) {
            for (ItemQuote item : store.items()) {
                quantities.merge(item.variantId(), item.quantity(), Math::addExact);
            }
        }
        return quantities;
    }

    public record StoreQuote(int storeId, int subtotal, int discountAmount, int totalAmount,
                             Integer specialDiscountId, Integer seasonalDiscountId,
                             List<ItemQuote> items) {
        public StoreQuote {
            items = List.copyOf(items);
        }
    }

    public record ItemQuote(int variantId, int quantity, int unitPrice, int totalAmount) { }

}
