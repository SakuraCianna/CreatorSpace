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
class DataCapabilityMigrationTests extends PostgresIntegrationTestSupport {

    @Container
    private static final PostgreSQLContainer POSTGRES = createPostgres("creatorspace_data_capability_test");

    // 验证 D 同学 P0-1 数据能力模块依赖的表、字段、唯一约束和索引。
    @Test
    void migrationsCreateDataCapabilityTablesColumnsAndIndexes() throws Exception {
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
                    "visit_logs",
                    "search_logs",
                    "operation_logs",
                    "content_statistics",
                    "daily_metrics",
                    "ai_agent_tasks",
                    "ai_agent_messages",
                    "ai_suggestions"
            );
            assertThat(columnNames(connection, "articles"))
                    .contains("view_count", "like_count", "comment_count");
            assertThat(columnNames(connection, "content_statistics"))
                    .contains(
                            "target_type",
                            "target_id",
                            "view_count",
                            "like_count",
                            "favorite_count",
                            "comment_count",
                            "last_viewed_at",
                            "updated_at"
                    );
            assertThat(uniqueConstraintColumns(connection, "content_statistics"))
                    .contains("target_type,target_id");
            assertThat(indexNames(connection)).contains(
                    "idx_visit_logs_created_at",
                    "idx_visit_logs_target",
                    "idx_search_logs_created_at",
                    "idx_search_logs_keyword_created_at",
                    "idx_operation_logs_created_at",
                    "idx_operation_logs_module_created_at",
                    "idx_operation_logs_operator_created_at",
                    "idx_ai_agent_tasks_status_created_at",
                    "idx_ai_suggestions_status_created_at"
            );
        }
    }

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

    private Set<String> uniqueConstraintColumns(Connection connection, String tableName) throws Exception {
        try (var statement = connection.prepareStatement("""
                select string_agg(kcu.column_name, ',' order by kcu.ordinal_position) as columns
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_name = kcu.constraint_name
                 and tc.table_schema = kcu.table_schema
                where tc.table_schema = 'public'
                  and tc.table_name = ?
                  and tc.constraint_type = 'UNIQUE'
                group by tc.constraint_name
                """)) {
            statement.setString(1, tableName);
            try (var rs = statement.executeQuery()) {
                Set<String> names = new HashSet<>();
                while (rs.next()) {
                    names.add(rs.getString("columns"));
                }
                return names;
            }
        }
    }

    private Set<String> indexNames(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("""
                     select indexname
                     from pg_indexes
                     where schemaname = 'public'
                     """)) {
            Set<String> names = new HashSet<>();
            while (rs.next()) {
                names.add(rs.getString("indexname"));
            }
            return names;
        }
    }
}
