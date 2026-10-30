package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.others.checkout.CheckoutBaseProductVariantModel;
import com.clothingstore.shop.dto.others.checkout.CheckoutBaseStoreOrderModel;
import com.clothingstore.shop.dto.request.checkout.ConfirmAmountRequestDTO;
import com.clothingstore.shop.exceptions.SharedException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutValidationTest {
    private final CheckoutService service = new CheckoutService(null, null, null, null, null, null);

    @Test
    void zeroAndNegativeQuantitiesAreRejectedBeforeAnyStockMovement() {
        for (int quantity : new int[]{0, -1}) {
            assertThatThrownBy(() -> service.validateOrder(request(item(12, quantity))))
                    .isInstanceOf(SharedException.class);
        }
    }

    @Test
    void duplicateVariantIdsAreRejectedBeforeAnyStockMovement() {
        assertThatThrownBy(() -> service.validateOrder(request(item(12, 1), item(12, 2))))
                .isInstanceOf(SharedException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void emptyOrderIsRejected() {
        assertThatThrownBy(() -> service.validateOrder(new ConfirmAmountRequestDTO()))
                .isInstanceOf(SharedException.class);
    }

    private ConfirmAmountRequestDTO request(CheckoutBaseProductVariantModel... items) {
        CheckoutBaseStoreOrderModel store = new CheckoutBaseStoreOrderModel();
        store.setStore_id(8);
        store.setProduct_variants(List.of(items));
        ConfirmAmountRequestDTO request = new ConfirmAmountRequestDTO();
        request.setStore_orders(List.of(store));
        return request;
    }

    private CheckoutBaseProductVariantModel item(int id, int quantity) {
        CheckoutBaseProductVariantModel item = new CheckoutBaseProductVariantModel();
        item.setProduct_variant_id(id);
        item.setQuantity(quantity);
        return item;
    }
}
