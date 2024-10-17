package com.clothingstore.shop.repository;

import com.clothingstore.shop.model.User;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.clothingstore.shop.jooq.tables.Users.USERS;

@Repository
public class AuthRepository {

    private final DSLContext dsl;

    @Autowired
    public AuthRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<User> findByUserAccount(String userAccount) {
        Record record = dsl.selectFrom(USERS)
                .where(USERS.ACCOUNT.eq(userAccount))
                .fetchOne();
        return Optional.ofNullable(record).map(this::mapToUser);
    }

    private User mapToUser(Record record) {
        return new User(
                record.get(USERS.ID),
                record.get(USERS.ACCOUNT),
                record.get(USERS.PASSWORD),
                record.get(USERS.EMAIL),
                record.get(USERS.IS_DISABLE),
                record.get(USERS.USER_TYPE)
        );
    }
}
