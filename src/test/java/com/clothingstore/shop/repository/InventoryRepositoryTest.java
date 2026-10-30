package com.clothingstore.shop.repository;

import com.clothingstore.shop.exceptions.SharedException;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryRepositoryTest {
    @Test
    void releasingStockUpdatesOnlyTheReservedVariant() throws Exception {
        AtomicReference<String> renderedSql = new AtomicReference<>();
        DSLContext dsl = DSL.using(new MockConnection(context -> {
            renderedSql.set(context.sql());
            return new MockResult[]{new MockResult(1)};
        }), SQLDialect.POSTGRES);

        new InventoryRepository(dsl).rollbackStock(dsl, 17, 2);

        String predicate = renderedSql.get().toLowerCase().split("where", 2)[1];
        assertThat(predicate).contains("product_variant_id").contains("reserved_stock");
    }

    @Test
    void releasingMissingReservedStockFailsInsteadOfSilentlyChangingInventory() {
        DSLContext dsl = DSL.using(new MockConnection(context ->
                new MockResult[]{new MockResult(0)}), SQLDialect.POSTGRES);

        assertThatThrownBy(() -> new InventoryRepository(dsl).rollbackStock(dsl, 17, 2))
                .isInstanceOf(SharedException.class)
                .hasMessageContaining("inconsistent");
    }
}
