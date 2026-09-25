package com.clothingstore.shop;

import static com.clothingstore.shop.jooq.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
@Sql("classpath:db/deployment-fixture.sql")
class ClothingShopApplicationTests {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>(
            System.getProperty("postgres.image", "postgres:16-alpine"));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
    }

    @Autowired DSLContext db;
    @Autowired TestRestTemplate http;

    @Test
    void emptyDatabaseIsMigratedAndGeneratedSourcesCanQueryFixtures() {
        assertThat(db.fetchCount(USERS, USERS.ACCOUNT.eq("deployment-fixture"))).isEqualTo(1);
        assertThat(db.fetchCount(USERS, USERS.ACCOUNT.eq("demo"))).isEqualTo(3);
        assertThat(db.fetchOne("select count(*) from product").get(0, Integer.class)).isPositive();
        assertThat(db.fetchOne("select count(*) from databasechangelog").get(0, Integer.class)).isPositive();
    }

    @Test
    void postgresTriggersAreInstalledByTheSameMigrations() {
        var userId = db.select(USERS.USER_ID).from(USERS)
                .where(USERS.ACCOUNT.eq("deployment-fixture")).fetchSingle(USERS.USER_ID);
        db.update(USERS).set(USERS.PHONE_NUMBER, "0900000000")
                .where(USERS.USER_ID.eq(userId)).execute();
        assertThat(db.fetchOne("select count(*) from user_log where fk_user_id = ? "
                + "and action = 'Update user columns:phone_number'", userId)
                .get(0, Integer.class)).isEqualTo(1);
    }

    @Test
    void deploymentHealthEndpointIsReadyAfterDatabaseInitialization() {
        var response = http.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
