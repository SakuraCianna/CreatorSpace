package com.creatorspace;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CreatorSpace 后端应用入口。
 */
@EnableScheduling
@SpringBootApplication
public class CreatorSpaceApplication {

    // 启动 CreatorSpace 后端应用。
    public static void main(String[] args) {
        SpringApplication.run(CreatorSpaceApplication.class, args);
    }

    // 在迁移前执行 repair，自动修复由于 CRLF 换行符差异或文件微调导致的 Flyway 校验和不一致问题。
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}

