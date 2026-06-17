package com.creatorspace.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Web 配置单元测试。
 */
class WebConfigTests {

    // 验证本地静态资源目录 URI 保留目录结尾斜杠，避免 Spring 运行时自动修正。
    @Test
    void directoryLocationEndsWithTrailingSlash() {
        Path root = Path.of("storage", "uploads").toAbsolutePath().normalize();

        assertThat(WebConfig.toDirectoryLocation(root))
                .isEqualTo(root.toUri() + "/");
    }
}
