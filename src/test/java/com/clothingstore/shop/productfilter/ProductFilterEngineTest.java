package com.clothingstore.shop.productfilter;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductFilterEngineTest {
    private ProductFilterRegistry registry;
    private ProductFilterValidator validator;
    private ProductFilterPlanner planner;

    @BeforeEach
    void setUp() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new YamlPropertySourceLoader()
                .load("catalog", new ClassPathResource("application.yml")).get(0));
        ProductFilterProperties properties = Binder.get(environment)
                .bind("catalog.filters", Bindable.of(ProductFilterProperties.class))
                .orElseThrow(() -> new IllegalStateException("Filter configuration is missing"));
        registry = new ProductFilterRegistry(properties);
        validator = new ProductFilterValidator(registry);
        planner = new ProductFilterPlanner();
    }

    @Test
    void schemaExposesOnlyConfiguredCategoryFields() {
        assertThat(registry.forCategory("Cloth")).extracting(FilterDefinition::key)
                .contains("name", "price", "inStock", "rating", "color", "size", "material")
                .doesNotContain("fit", "closure", "style");
        assertThat(registry.forCategory("ALL")).extracting(FilterDefinition::key)
                .containsExactly("name", "price", "inStock", "rating");
    }

    @Test
    void rejectsFiltersOutsideTheirCategory() {
        assertThatThrownBy(() -> validator.validate(request("shoes",
                new FilterCriterion("fit", FilterOperator.EQ, List.of("Slim")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void rejectsUnsupportedOptionsAndOperators() {
        assertThatThrownBy(() -> validator.validate(request("cloth",
                new FilterCriterion("size", FilterOperator.EQ, List.of("XXXL")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate(request("cloth",
                new FilterCriterion("name", FilterOperator.GTE, List.of("Shirt")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateKeysAndInvalidBounds() {
        assertThatThrownBy(() -> validator.validate(new ProductFilterRequest("cloth", List.of(
                new FilterCriterion("name", FilterOperator.EQ, List.of("A")),
                new FilterCriterion("name", FilterOperator.EQ, List.of("B"))), null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("once");
        assertThatThrownBy(() -> validator.validate(request("cloth",
                new FilterCriterion("price", FilterOperator.BETWEEN, List.of("200", "100")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lower bound");
    }

    @Test
    void rejectsInvalidNumbersAndPaginationOverflow() {
        assertThatThrownBy(() -> validator.validate(request("cloth",
                new FilterCriterion("rating", FilterOperator.GTE, List.of("6")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate(new ProductFilterRequest("cloth", List.of(),
                "newest", Integer.MAX_VALUE, 100)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void combinesVariantFiltersAgainstOneVariant() {
        ProductFilterRequest request = new ProductFilterRequest("cloth", List.of(
                new FilterCriterion("price", FilterOperator.BETWEEN, List.of("100", "200")),
                new FilterCriterion("color", FilterOperator.IN, List.of("Black", "Blue")),
                new FilterCriterion("inStock", FilterOperator.EQ, List.of("true"))),
                "price_asc", 2, 15);
        ProductFilterPlan plan = planner.plan(validator.validate(request));
        String sql = DSL.using(SQLDialect.POSTGRES).renderInlined(plan.condition()).toLowerCase();
        assertThat(sql).contains("exists", "product_variant", "stock", "price", "color");
        assertThat(sql.split("exists", -1)).hasSize(2);
        assertThat(plan.offset()).isEqualTo(15);
        assertThat(plan.pageSize()).isEqualTo(15);
        assertThat(DSL.using(SQLDialect.POSTGRES).renderInlined(plan.matchedVariantCondition()))
                .contains("price", "color", "stock");
    }

    @Test
    void outOfStockRequiresNoAvailableVariant() {
        ProductFilterPlan plan = planner.plan(validator.validate(request("all",
                new FilterCriterion("inStock", FilterOperator.EQ, List.of("false")))));
        String sql = DSL.using(SQLDialect.POSTGRES).renderInlined(plan.condition()).toLowerCase();
        assertThat(sql).contains("not exists", "reserved_stock");
    }

    @Test
    void customFieldsUseCorrelatedExistsAndBoundKey() {
        ProductFilterPlan plan = planner.plan(validator.validate(request("shoes",
                new FilterCriterion("closure", FilterOperator.CONTAINS, List.of("lace")))));
        String sql = DSL.using(SQLDialect.POSTGRES).renderInlined(plan.condition()).toLowerCase();
        assertThat(sql).contains("product_filter_attribute", "attribute_key", "closure", "attribute_value");
    }

    @Test
    void rejectsMalformedCheckboxAndSelectValueCount() {
        assertThatThrownBy(() -> validator.validate(request("all",
                new FilterCriterion("inStock", FilterOperator.EQ, List.of("perhaps")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate(request("cloth",
                new FilterCriterion("color", FilterOperator.IN, List.of()))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ProductFilterRequest request(String category, FilterCriterion criterion) {
        return new ProductFilterRequest(category, List.of(criterion), null, null, null);
    }
}
