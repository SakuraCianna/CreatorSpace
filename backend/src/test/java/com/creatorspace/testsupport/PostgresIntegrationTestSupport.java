package com.creatorspace.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class PostgresIntegrationTestSupport {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("pgvector/pgvector:pg17")
            .asCompatibleSubstituteFor("postgres");

    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_PASSWORD = "admin-secret";
    protected static final String ADMIN_PASSWORD_HASH =
            "$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u";

    // 创建 PostgreSQL 测试容器，容器生命周期由测试类上的 @Container 托管。
    @SuppressWarnings("resource")
    protected static PostgreSQLContainer createPostgres(String databaseName) {
        return new PostgreSQLContainer(POSTGRES_IMAGE)
                .withDatabaseName(databaseName)
                .withUsername("creatorspace")
                .withPassword("creatorspace");
    }

    // 注册测试容器数据库连接属性。
    protected static void registerPostgresProperties(
            DynamicPropertyRegistry registry,
            PostgreSQLContainer postgres
    ) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("ADMIN_USERNAME", () -> ADMIN_USERNAME);
        registry.add("ADMIN_PASSWORD_HASH", () -> ADMIN_PASSWORD_HASH);
        registry.add("PASSWORD_BCRYPT_STRENGTH", () -> "4");
        registry.add("JWT_SECRET", () -> "test-secret-with-enough-length-for-hmac-signing");
        registry.add("JWT_ACCESS_TOKEN_EXPIRE_MINUTES", () -> "60");
    }
}
