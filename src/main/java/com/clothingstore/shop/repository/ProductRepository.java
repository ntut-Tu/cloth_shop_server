package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.repository.products.ProductDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.products.ProductSummaryRepositoryDTO;
import com.clothingstore.shop.dto.repository.products.ProductVariantRepositoryDTO;
import com.clothingstore.shop.dto.request.AddProductRequestDTO;
import com.clothingstore.shop.jooq.tables.Product;
import com.clothingstore.shop.jooq.tables.ProductVariant;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static com.clothingstore.shop.jooq.Tables.*;

@Repository
public class ProductRepository {

    private final DSLContext dsl;

    public ProductRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<ProductSummaryRepositoryDTO> fetchProductSummaries(int page, int pageSize) {
        int offset = (page - 1) * pageSize;

        return dsl.select(
                        PRODUCT.PRODUCT_ID,
                        PRODUCT.NAME,
                        PRODUCT.TOTAL_SALES,
                        PRODUCT.RATE,
                        PRODUCT.IMAGE_URL,
                        PRODUCT.CATEGORY,
                        VENDOR.STORE_DESCRIPTION
                )
                .from(PRODUCT)
                .join(VENDOR).on(PRODUCT.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                .where(PRODUCT.IS_LIST.isTrue())
                .orderBy(PRODUCT.PRODUCT_ID.asc())
                .limit(pageSize)
                .offset(offset)
                .fetchInto(ProductSummaryRepositoryDTO.class);
    }

    public ProductDetailRepositoryDTO fetchProductDetails(int productId) {
        // Step 1: Fetch the main product details and vendor info
        ProductDetailRepositoryDTO productDetail = dsl.select(
                        PRODUCT.PRODUCT_ID,
                        PRODUCT.NAME,
                        PRODUCT.DESCRIPTION,
                        PRODUCT.TOTAL_SALES,
                        PRODUCT.RATE,
                        PRODUCT.IMAGE_URL,
                        PRODUCT.CATEGORY,
                        PRODUCT.IS_LIST,
                        PRODUCT.FK_VENDOR_ID,
                        VENDOR.STORE_DESCRIPTION,
                        VENDOR.STORE_ADDRESS,
                        VENDOR.STORE_LOGO_URL
                )
                .from(PRODUCT)
                .join(VENDOR).on(PRODUCT.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                .where(PRODUCT.PRODUCT_ID.eq(productId))
                .fetchOneInto(ProductDetailRepositoryDTO.class);

        // Step 2: Fetch associated product variants based on PRODUCT_ID
        List<ProductVariantRepositoryDTO> variants = dsl.select(
                        PRODUCT_VARIANT.PRODUCT_VARIANT_ID,
                        PRODUCT_VARIANT.COLOR,
                        PRODUCT_VARIANT.STOCK,
                        PRODUCT_VARIANT.SIZE,
                        PRODUCT_VARIANT.PRICE
                )
                .from(PRODUCT_VARIANT)
                .where(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(productId))
                .fetchInto(ProductVariantRepositoryDTO.class);

        // Set the variants in product details
        try{
            productDetail.setProductVariantRepositoryDTO(variants);
        }catch (Exception e){
            productDetail.setProductVariantRepositoryDTO(null);
        }
        return productDetail;
    }

    public Integer addProduct(AddProductRequestDTO productRequestDTO) {
        // Step 1: Insert product data
        Integer productId = dsl.insertInto(Product.PRODUCT)
                .set(Product.PRODUCT.NAME, productRequestDTO.getName())
                .set(Product.PRODUCT.DESCRIPTION, productRequestDTO.getDescription())
                .set(Product.PRODUCT.TOTAL_SALES, 0)
                .set(Product.PRODUCT.RATE, BigDecimal.ZERO)
                .set(Product.PRODUCT.IMAGE_URL, productRequestDTO.getImageUrl())
                .set(Product.PRODUCT.CATEGORY, productRequestDTO.getCategory())
                .set(Product.PRODUCT.IS_LIST, productRequestDTO.getIsList())
                .set(Product.PRODUCT.FK_VENDOR_ID, productRequestDTO.getFkVendorId())
                .returning(Product.PRODUCT.PRODUCT_ID)
                .fetchOne()
                .getProductId();

        // Step 2: Insert product variants if available
        List<ProductVariantRepositoryDTO> variants = productRequestDTO.getProductVariants();
        if (variants != null && !variants.isEmpty()) {
            for (ProductVariantRepositoryDTO variant : variants) {
                dsl.insertInto(ProductVariant.PRODUCT_VARIANT)
                        .set(ProductVariant.PRODUCT_VARIANT.COLOR, variant.getColor())
                        .set(ProductVariant.PRODUCT_VARIANT.STOCK, variant.getStock())
                        .set(ProductVariant.PRODUCT_VARIANT.SIZE, variant.getSize())
                        .set(ProductVariant.PRODUCT_VARIANT.PRICE, variant.getPrice())
                        .set(ProductVariant.PRODUCT_VARIANT.FK_PRODUCT_ID, productId)
                        .execute();
            }
        }
        return productId;
    }
}
