package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.response.review.AddReviewResponseDTO;
import org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneJdbcType;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import static com.clothingstore.shop.jooq.Tables.*;

@Repository
public class ReviewRepository {
    private final DSLContext dsl;

    public ReviewRepository(DSLContext dsl) {
        this.dsl = dsl;
    }
    public AddReviewResponseDTO addReview(Integer userId, Integer productId, String comment, BigDecimal rating) throws Exception {
        try {
            Record product =dsl.select(PRODUCT.PRODUCT_ID)
                    .from(PRODUCT)
                    .where(PRODUCT.PRODUCT_ID.eq(productId))
                    .fetchOne();
            Integer customer_id = dsl.select(CUSTOMER.CUSTOMER_ID)
                    .from(CUSTOMER)
                    .join(USERS).on(USERS.USER_ID.eq(CUSTOMER.FK_USER_ID))
                    .where(USERS.USER_ID.eq(userId))
                    .fetchOne()
                    .into(Integer.class);
            if(product != null){
                boolean isReviewExist = dsl.fetchExists(REVIEW, REVIEW.FK_CUSTOMER_ID.eq(customer_id).and(REVIEW.FK_PRODUCT_ID.eq(productId)));
                if(isReviewExist){
                    throw new IllegalArgumentException("Review already exists.");
                }
                return dsl.insertInto(REVIEW)
                        .set(REVIEW.FK_CUSTOMER_ID, customer_id)
                        .set(REVIEW.FK_PRODUCT_ID, productId)
                        .set(REVIEW.COMMENT, comment)
                        .set(REVIEW.RATE, rating)
                        .returning(REVIEW.REVIEW_ID, REVIEW.COMMENT)
                        .fetchOneInto(AddReviewResponseDTO.class);
            }else {
                throw new IllegalArgumentException("Product not found.");
            }
        }catch (Exception e){
            throw e;
        }
    }
}
