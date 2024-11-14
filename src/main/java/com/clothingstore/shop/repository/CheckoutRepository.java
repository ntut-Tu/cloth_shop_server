package com.clothingstore.shop.repository;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class CheckoutRepository {
    private final DSLContext dsl;
    @Autowired
    public CheckoutRepository(DSLContext dslContext) {
        this.dsl = dslContext;
    }

}
