package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.response.userData.EditUserDataResponseDTO;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static com.clothingstore.shop.jooq.Tables.USERS;

@Repository
public class EditUserDataRepository {
    private final DSLContext dsl;

    public EditUserDataRepository(DSLContext dsl) {
        this.dsl = dsl;
    }
    public EditUserDataResponseDTO editUserData(String account, String password, String email, String phone_number) throws Exception {
        try {
            return dsl.update(USERS)
                    .set(USERS.ACCOUNT, account)
                    .set(USERS.PASSWORD, password)
                    .set(USERS.EMAIL, email)
                    .set(USERS.PHONE_NUMBER, phone_number)
                    .returning(USERS.ACCOUNT, USERS.EMAIL, USERS.PHONE_NUMBER)
                    .fetchOneInto(EditUserDataResponseDTO.class);
        }catch (Exception e){
            throw e;
        }
    }
}
