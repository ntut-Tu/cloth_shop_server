package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.repository.products.ProductDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.products.ProductSummaryRepositoryDTO;
import com.clothingstore.shop.dto.repository.products.ProductVariantRepositoryDTO;
import com.clothingstore.shop.dto.request.AddProductRequestDTO;
import com.clothingstore.shop.dto.request.product.FetchProductsParams;
import com.clothingstore.shop.dto.response.product.PaginatedResponse;
import com.clothingstore.shop.dto.response.product.ProductInfoResponseDTO;
import com.clothingstore.shop.dto.response.product.ProductSummaryV2ResponseDTO;
import com.clothingstore.shop.enums.CategorizedProduct;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.jooq.tables.Product;
import com.clothingstore.shop.jooq.tables.ProductVariant;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.clothingstore.shop.jooq.Tables.*;
import static com.clothingstore.shop.utils.EnumUtils.isStringInEnum;

@Repository
public class ProductRepository {

    private final DSLContext dsl;

    public ProductRepository(DSLContext dsl) {
        this.dsl = dsl;
    }


    /**
     * 取得特定產品的詳細資訊。
     *
     * @param productId 產品 ID
     * @return 產品詳細資訊
     */
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
    /**
     * 新增產品到資料庫。
     *
     * @param productRequestDTO 產品資料
     * @return 新增產品的 ID
     */
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

    public PaginatedResponse<ProductSummaryV2ResponseDTO> fetchProductsV2(FetchProductsParams fetchParams, Integer userId) {
        try{
            // 构建基础条件
            Condition baseCondition = PRODUCT.IS_LIST.isTrue();

            // 添加分类条件
            if (fetchParams.getCategory() != null && !fetchParams.getCategory().isEmpty()) {
                if (fetchParams.getCategory().equalsIgnoreCase(CategorizedProduct.ALL.toString())) {
                    // 不添加任何分类条件
                } else if (isStringInEnum(fetchParams.getCategory(), CategorizedProduct.class)) {
                    baseCondition = baseCondition.and(PRODUCT.CATEGORY.equalIgnoreCase(fetchParams.getCategory()));
                } else {
                    // TODO: Not working as expected
                    baseCondition = baseCondition.and(
                            PRODUCT.CATEGORY.notIn(
                                    Arrays.stream(CategorizedProduct.values())
                                            .map(Enum::name)
                                            .map(String::toLowerCase)
                                            .collect(Collectors.toList())
                            )
                    );
                }
            }

            // 添加搜索条件
            if (fetchParams.getSearch() != null && !fetchParams.getSearch().isEmpty()) {
                baseCondition = baseCondition.and(PRODUCT.NAME.likeIgnoreCase("%" + fetchParams.getSearch() + "%"));
            }
            switch (fetchParams.getRole()){
                case "admin":
                    break;
                case "vendor":
                    Integer vendorId = dsl.select(VENDOR.VENDOR_ID)
                            .from(VENDOR)
                            .where(VENDOR.FK_USER_ID.eq(userId))
                            .fetchOneInto(Integer.class);
                    baseCondition=baseCondition.and(PRODUCT.FK_VENDOR_ID.eq(vendorId));
                    break;
                default:
                    baseCondition=baseCondition.and(PRODUCT.IS_LIST.isTrue());
                    break;
            }
            // 计算总记录数
            int totalRecords = dsl.select(DSL.countDistinct(PRODUCT.PRODUCT_ID))
                    .from(PRODUCT)
                    .join(VENDOR).on(PRODUCT.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                    .join(USERS).on(VENDOR.FK_USER_ID.eq(USERS.USER_ID))
                    .leftJoin(PRODUCT_VARIANT).on(PRODUCT.PRODUCT_ID.eq(PRODUCT_VARIANT.FK_PRODUCT_ID))
                    .where(baseCondition)
                    .fetchOne(0, int.class);

            // 构建分页数据查询
            SelectQuery<Record10<Integer, String, Integer, BigDecimal, String, String, String, String, Integer, Integer>> query = dsl.select(
                            PRODUCT.PRODUCT_ID,
                            PRODUCT.NAME,
                            PRODUCT.TOTAL_SALES,
                            PRODUCT.RATE,
                            PRODUCT.IMAGE_URL,
                            PRODUCT.CATEGORY,
                            USERS.ACCOUNT.as("storeName"),
                            USERS.PROFILE_PIC_URL.as("storeImageUrl"),
                            DSL.min(PRODUCT_VARIANT.PRICE).as("minPrice"),
                            DSL.max(PRODUCT_VARIANT.PRICE).as("maxPrice")
                    )
                    .from(PRODUCT)
                    .join(VENDOR).on(PRODUCT.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                    .join(USERS).on(VENDOR.FK_USER_ID.eq(USERS.USER_ID))
                    .leftJoin(PRODUCT_VARIANT).on(PRODUCT.PRODUCT_ID.eq(PRODUCT_VARIANT.FK_PRODUCT_ID))
                    .where(baseCondition)
                    .groupBy(
                            PRODUCT.PRODUCT_ID,
                            PRODUCT.NAME,
                            PRODUCT.TOTAL_SALES,
                            PRODUCT.RATE,
                            PRODUCT.IMAGE_URL,
                            PRODUCT.CATEGORY,
                            USERS.ACCOUNT,
                            USERS.PROFILE_PIC_URL
                    )
                    .getQuery();

            // 添加排序条件
            if (fetchParams.getSort() != null) {
                switch (fetchParams.getSort()) {
                    case "sold":
                        query.addOrderBy(PRODUCT.TOTAL_SALES.desc());
                        break;
                    case "price_asc":
                        query.addOrderBy(DSL.min(PRODUCT_VARIANT.PRICE).asc());
                        break;
                    case "price_desc":
                        query.addOrderBy(DSL.max(PRODUCT_VARIANT.PRICE).desc());
                        break;
                    case "rate":
                        query.addOrderBy(PRODUCT.RATE.desc());
                        break;
                    default:
                        query.addOrderBy(PRODUCT.PRODUCT_ID.asc());
                }
            } else {
                query.addOrderBy(PRODUCT.PRODUCT_ID.asc());
            }

            // 设置分页
            int offset = (fetchParams.getPage() - 1) * fetchParams.getPageSize();
            query.addLimit(fetchParams.getPageSize());
            query.addOffset(offset);

            // 执行分页查询
            List<ProductSummaryV2ResponseDTO> products = query.fetchInto(ProductSummaryV2ResponseDTO.class);

            // 返回分页响应
            return new PaginatedResponse<>(products, totalRecords);
        } catch (Exception e) {
            throw e;
        }
    }

    public List<ProductInfoResponseDTO> getProductListForCoupon(Integer vendorId) {
        return dsl.select(
                        PRODUCT_VARIANT.PRODUCT_VARIANT_ID.as("productVariantId"),
                        PRODUCT.NAME.as("productName"),
                        PRODUCT_VARIANT.COLOR.as("color"),
                        PRODUCT_VARIANT.SIZE.as("size")
                )
                .from(PRODUCT)
                .leftJoin(PRODUCT_VARIANT).on(PRODUCT.PRODUCT_ID.eq(PRODUCT_VARIANT.FK_PRODUCT_ID))
                .where(PRODUCT.FK_VENDOR_ID.eq(vendorId))
                .fetchInto(ProductInfoResponseDTO.class);
    }

    public void updateProductStock(Integer vendorId,Integer productVariantId,Integer newStock) {
        if(dsl.select(PRODUCT_VARIANT.PRODUCT_VARIANT_ID)
                .from(PRODUCT_VARIANT)
                .join(PRODUCT).on(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(PRODUCT.PRODUCT_ID))
                .join(VENDOR).on(PRODUCT.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariantId))
                .and(VENDOR.FK_USER_ID.eq(vendorId))
                .fetchOneInto(Integer.class)==null){
            throw new IllegalArgumentException("Product variant not found");
        }
        dsl.update(PRODUCT_VARIANT)
                .set(PRODUCT_VARIANT.STOCK,newStock )
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariantId))
                .execute();
    }

    public void updateProductStatus(Integer vendorId, Integer productVariantId, Boolean updatedStatus) {
        if(dsl.select(PRODUCT_VARIANT.PRODUCT_VARIANT_ID)
                .from(PRODUCT_VARIANT)
                .join(PRODUCT).on(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(PRODUCT.PRODUCT_ID))
                .join(VENDOR).on(PRODUCT.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariantId))
                .and(VENDOR.FK_USER_ID.eq(vendorId))
                .fetchOneInto(Integer.class)==null){
            throw new IllegalArgumentException("Product variant not found");
        }
        dsl.update(PRODUCT)
                .set(PRODUCT.IS_LIST, updatedStatus)
                .where(PRODUCT.PRODUCT_ID.eq(
                        dsl.select(PRODUCT_VARIANT.FK_PRODUCT_ID)
                                .from(PRODUCT_VARIANT)
                                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariantId))
                ))
                .execute();
    }
}
