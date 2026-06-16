package com.creatorspace.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class FlywayMigrationTests {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("pgvector/pgvector:pg17")
            .asCompatibleSubstituteFor("postgres");
    private static final String TEST_ADMIN_PASSWORD_HASH =
            "$2b$" + "12$012345678901234567890u7Tw4SNuzxpOhOwdzH7Y0yynzJKCwU9W";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("creatorspace_test")
            .withUsername("creatorspace")
            .withPassword("creatorspace");

    @Test
    void migrationsCreateCoreSchemaExtensionsAndSeededAdmin() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .placeholders(java.util.Map.of(
                        "adminUsername", "admin",
                        "adminPasswordHash", TEST_ADMIN_PASSWORD_HASH
                ))
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        )) {
            assertThat(tableNames(connection)).contains(
                    "users",
                    "roles",
                    "user_roles",
                    "articles",
                    "portfolio_projects",
                    "theme_configs",
                    "file_resources",
                    "operation_logs"
            );
            assertThat(extensionNames(connection)).contains("pg_trgm", "pgcrypto");
            assertThat(singleLong(connection, "select count(*) from roles where code in ('ADMIN', 'USER')")).isEqualTo(2L);
            assertThat(singleLong(connection, "select count(*) from users where username = 'admin'")).isEqualTo(1L);
        }
    }

    private Set<String> tableNames(java.sql.Connection connection) throws Exception {
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("""
                     select table_name
                     from information_schema.tables
                     where table_schema = 'public'
                     """)) {
            var names = new java.util.HashSet<String>();
            while (rs.next()) {
                names.add(rs.getString("table_name"));
            }
            return names;
        }
    }

    private Set<String> extensionNames(java.sql.Connection connection) throws Exception {
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("select extname from pg_extension")) {
            var names = new java.util.HashSet<String>();
            while (rs.next()) {
                names.add(rs.getString("extname"));
            }
            return names;
        }
    }

    private long singleLong(java.sql.Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
