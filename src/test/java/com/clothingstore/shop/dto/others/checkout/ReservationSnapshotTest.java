package com.clothingstore.shop.dto.others.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationSnapshotTest {
    @Test
    void persistedQuoteRetainsPricesDiscountsAndLineItems() throws Exception {
        ReservationSnapshot original = new ReservationSnapshot(5, 600, 60, 10, 20, 630,
                31, List.of(new ReservationSnapshot.StoreQuote(8, 600, 20, 580,
                41, null, List.of(new ReservationSnapshot.ItemQuote(10, 2, 300, 600)))));

        ReservationSnapshot restored = new ObjectMapper().readValue(
                new ObjectMapper().writeValueAsString(original), ReservationSnapshot.class);

        assertThat(restored).isEqualTo(original);
        assertThat(restored.quantitiesByVariant()).containsEntry(10, 2);
    }

    @Test
    void quantitiesAreAggregatedAndSortedForDeterministicStockLocking() {
        ReservationSnapshot quote = new ReservationSnapshot(5, 0, 0, 0, 0, 0, null,
                List.of(new ReservationSnapshot.StoreQuote(8, 0, 0, 0, null, null,
                        List.of(new ReservationSnapshot.ItemQuote(9, 1, 0, 0),
                                new ReservationSnapshot.ItemQuote(2, 3, 0, 0),
                                new ReservationSnapshot.ItemQuote(9, 2, 0, 0)))));

        assertThat(quote.quantitiesByVariant().keySet()).containsExactly(2, 9);
        assertThat(quote.quantitiesByVariant()).containsEntry(9, 3).containsEntry(2, 3);
    }
}
