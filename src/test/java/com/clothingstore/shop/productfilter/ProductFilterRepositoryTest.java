package com.clothingstore.shop.productfilter;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductFilterRepositoryTest {
    @Test
    void countsProductsSeparatelyFromVariantJoinAndPaginatesResults() {
        List<String> statements = new ArrayList<>();
        DSLContext records = DSL.using(SQLDialect.POSTGRES);
        MockConnection connection = new MockConnection(context -> {
            statements.add(context.sql());
            if (statements.size() == 1) {
                Field<Integer> countField = DSL.field("count", Integer.class);
                Result<Record1<Integer>> result = records.newResult(countField);
                Record1<Integer> row = records.newRecord(countField);
                row.value1(2);
                result.add(row);
                return new MockResult[] { new MockResult(1, result) };
            }
            return new MockResult[] { new MockResult(0, records.newResult()) };
        });
        ProductFilterRepository repository = new ProductFilterRepository(DSL.using(connection, SQLDialect.POSTGRES));
        ProductFilterPlan plan = new ProductFilterPlan(DSL.trueCondition(), DSL.trueCondition(),
                List.of(com.clothingstore.shop.jooq.Tables.PRODUCT.PRODUCT_ID.desc()), 10, 20);

        var page = repository.search(plan);

        assertThat(page.getTotalRecords()).isEqualTo(2);
        assertThat(page.getItems()).isEmpty();
        assertThat(statements).hasSize(2);
        assertThat(statements.get(0).toLowerCase()).doesNotContain("product_variant");
        assertThat(statements.get(1).toLowerCase()).contains("product_variant", "offset", "fetch next");
    }
}
