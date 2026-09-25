package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.response.users.UserSummaryResponseDTO;
import com.clothingstore.shop.dto.response.users.UserLogResponseDTO;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.clothingstore.shop.jooq.tables.UserLog.*;
import static com.clothingstore.shop.jooq.tables.Users.*;
@Repository
public class UserRepository {
    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<UserSummaryResponseDTO> fetchUsers(Integer page, Integer size) {
        return dsl.select(
                USERS.USER_ID.as("id"),
                USERS.ACCOUNT.as("username"),
                USERS.EMAIL.as("email"),
                USERS.USER_TYPE.as("role"),
                USERS.CREATED_AT.as("establishDate"),
                USERS.IS_ACTIVE.as("isActive"))
                .from(USERS)
                .limit(size)
                .offset(page * size)
                .fetchInto(UserSummaryResponseDTO.class);
    }

    public Boolean banUser(Integer banUserId) {
        return dsl.update(USERS)
                .set(USERS.IS_ACTIVE, false)
                .where(USERS.USER_ID.eq(banUserId))
                .execute() == 1;
    }

    public List<UserLogResponseDTO> fetchUserLogs(int page, int size) {
        return dsl.select(
                USER_LOG.USER_LOG_ID.as("log_id"),
                USER_LOG.FK_USER_ID.as("user_id"),
                USERS.ACCOUNT.as("username"),
                USER_LOG.ACTION.as("action"),
                USER_LOG.DATE.as("date"))
                .from(USER_LOG)
                .join(USERS).on(USER_LOG.FK_USER_ID.eq(USERS.USER_ID))
                .limit(size)
                .offset(page * size)
                .fetchInto(UserLogResponseDTO.class);
    }
}
