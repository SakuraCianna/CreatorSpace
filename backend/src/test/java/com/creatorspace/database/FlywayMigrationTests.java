package com.creatorspace.database;

import com.creatorspace.testsupport.PostgresIntegrationTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class FlywayMigrationTests extends PostgresIntegrationTestSupport {

    @Container
    private static final PostgreSQLContainer POSTGRES = createPostgres("creatorspace_migration_test");

    // 验证迁移能创建核心表、扩展和管理员种子数据。
    @Test
    void migrationsCreateCoreSchemaExtensionsAndSeededAdmin() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .placeholders(Map.of(
                        "adminUsername", ADMIN_USERNAME,
                        "adminPasswordHash", ADMIN_PASSWORD_HASH
                ))
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        )) {
            assertThat(tableNames(connection)).contains(
                    "users",
                    "roles",
                    "user_roles",
                    "user_friendships",
                    "articles",
                    "article_visibility_users",
                    "portfolio_projects",
                    "theme_configs",
                    "file_resources",
                    "file_resource_references",
                    "sensitive_words",
                    "operation_logs"
            );
            assertThat(extensionNames(connection)).contains("pg_trgm", "pgcrypto", "vector");
            assertThat(columnNames(connection, "users")).doesNotContain("email");
            assertThat(nullableColumnNames(connection, "users")).contains("nickname");
            assertThat(columnNames(connection, "articles"))
                    .contains("privacy_type")
                    .doesNotContain("access_password_hash");
            assertThat(columnNames(connection, "comments"))
                    .contains("root_id", "reply_to_user_id", "depth", "reply_count")
                    .doesNotContain("nickname", "email");
            assertThat(columnNames(connection, "like_records")).doesNotContain("guest_key");
            assertThat(primaryKeyColumns(connection, "article_visibility_users"))
                    .containsExactlyInAnyOrder("article_id", "user_id");
            assertThat(tableNamesWithoutComments(connection)).isEmpty();
            assertThat(columnNamesWithoutComments(connection)).isEmpty();
            assertThat(singleLong(connection, "select count(*) from roles where code in ('ADMIN', 'USER')")).isEqualTo(2L);
            assertThat(singleLong(connection, "select count(*) from users where username = 'admin'")).isEqualTo(1L);
            assertShowcaseDataSeeded(connection);
        }
    }

    // 验证 V2 展示数据覆盖核心业务表和主要关联表。
    private void assertShowcaseDataSeeded(Connection connection) throws Exception {
        assertTablesContainRows(
                connection,
                "roles",
                "users",
                "user_roles",
                "user_friendships",
                "categories",
                "tags",
                "articles",
                "article_visibility_users",
                "article_versions",
                "article_tags",
                "portfolio_projects",
                "project_tags",
                "project_images",
                "inspiration_cards",
                "inspiration_tags",
                "comments",
                "sensitive_words",
                "theme_configs",
                "site_configs",
                "file_resources",
                "file_resource_references",
                "visit_logs",
                "like_records",
                "favorite_records",
                "friend_links",
                "operation_logs"
        );
        assertThat(countRows(connection, "articles")).isGreaterThanOrEqualTo(18L);
        assertThat(countRows(connection, "portfolio_projects")).isGreaterThanOrEqualTo(12L);
        assertThat(countRows(connection, "inspiration_cards")).isGreaterThanOrEqualTo(14L);
        assertThat(countRows(connection, "visit_logs")).isGreaterThanOrEqualTo(80L);
        assertThat(countRows(connection, "operation_logs")).isGreaterThanOrEqualTo(36L);
    }

    // 验证指定表都至少有一条展示数据。
    private void assertTablesContainRows(Connection connection, String... tableNames) throws Exception {
        for (String tableName : tableNames) {
            assertThat(countRows(connection, tableName))
                    .describedAs(tableName)
                    .isPositive();
        }
    }

    // 读取当前 schema 的表名集合。
    private Set<String> tableNames(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("""
                     select table_name
                     from information_schema.tables
                     where table_schema = 'public'
                     """)) {
            Set<String> names = new HashSet<>();
            while (rs.next()) {
                names.add(rs.getString("table_name"));
            }
            return names;
        }
    }

    // 读取指定表的列名集合。
    private Set<String> columnNames(Connection connection, String tableName) throws Exception {
        try (var statement = connection.prepareStatement("""
                select column_name
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = ?
                """)) {
            statement.setString(1, tableName);
            try (var rs = statement.executeQuery()) {
                Set<String> names = new HashSet<>();
                while (rs.next()) {
                    names.add(rs.getString("column_name"));
                }
                return names;
            }
        }
    }

    // 读取指定表可为空的列名集合。
    private Set<String> nullableColumnNames(Connection connection, String tableName) throws Exception {
        try (var statement = connection.prepareStatement("""
                select column_name
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = ?
                  and is_nullable = 'YES'
                """)) {
            statement.setString(1, tableName);
            try (var rs = statement.executeQuery()) {
                Set<String> names = new HashSet<>();
                while (rs.next()) {
                    names.add(rs.getString("column_name"));
                }
                return names;
            }
        }
    }

    // 读取指定表的主键列集合。
    private Set<String> primaryKeyColumns(Connection connection, String tableName) throws Exception {
        try (var statement = connection.prepareStatement("""
                select kcu.column_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_name = kcu.constraint_name
                 and tc.table_schema = kcu.table_schema
                where tc.table_schema = 'public'
                  and tc.table_name = ?
                  and tc.constraint_type = 'PRIMARY KEY'
                """)) {
            statement.setString(1, tableName);
            try (var rs = statement.executeQuery()) {
                Set<String> names = new HashSet<>();
                while (rs.next()) {
                    names.add(rs.getString("column_name"));
                }
                return names;
            }
        }
    }

    // 读取已启用的 PostgreSQL 扩展集合。
    private Set<String> extensionNames(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("select extname from pg_extension")) {
            Set<String> names = new HashSet<>();
            while (rs.next()) {
                names.add(rs.getString("extname"));
            }
            return names;
        }
    }

    // 读取缺少表注释的业务表集合。
    private Set<String> tableNamesWithoutComments(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("""
                     select c.relname
                     from pg_class c
                     join pg_namespace n on n.oid = c.relnamespace
                     left join pg_description d on d.objoid = c.oid and d.objsubid = 0
                     where n.nspname = 'public'
                       and c.relkind = 'r'
                       and c.relname <> 'flyway_schema_history'
                       and d.description is null
                     """)) {
            Set<String> names = new HashSet<>();
            while (rs.next()) {
                names.add(rs.getString("relname"));
            }
            return names;
        }
    }

    // 读取缺少字段注释的业务字段集合。
    private Set<String> columnNamesWithoutComments(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("""
                     select c.relname || '.' || a.attname as column_name
                     from pg_class c
                     join pg_namespace n on n.oid = c.relnamespace
                     join pg_attribute a on a.attrelid = c.oid
                     left join pg_description d on d.objoid = c.oid and d.objsubid = a.attnum
                     where n.nspname = 'public'
                       and c.relkind = 'r'
                       and c.relname <> 'flyway_schema_history'
                       and a.attnum > 0
                       and not a.attisdropped
                       and d.description is null
                     """)) {
            Set<String> names = new HashSet<>();
            while (rs.next()) {
                names.add(rs.getString("column_name"));
            }
            return names;
        }
    }

    // 执行单值统计 SQL。
    private long singleLong(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    // 读取指定表的数据量。
    private long countRows(Connection connection, String tableName) throws Exception {
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("select count(*) from " + tableName)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
