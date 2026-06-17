package com.creatorspace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Web 跨域和本地静态资源映射配置。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Value("${app.storage.local-root:storage/uploads}")
    private String localStorageRoot;

    @Value("${app.storage.public-prefix:/uploads}")
    private String publicPrefix;

    // 注册前后端跨域规则。
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(split(allowedOrigins))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    // 注册本地上传文件的静态访问路径。
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = publicPrefix.startsWith("/") ? publicPrefix : "/" + publicPrefix;
        Path root = resolveFromProjectRoot(localStorageRoot);
        registry.addResourceHandler(prefix + "/**")
                .addResourceLocations(toDirectoryLocation(root));
    }

    // 将相对存储路径解析到项目根目录。
    private static Path resolveFromProjectRoot(String value) {
        Path configured = Path.of(value);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }

        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (cwd.getFileName() != null && "backend".equalsIgnoreCase(cwd.getFileName().toString())) {
            Path parent = cwd.getParent();
            if (parent != null) {
                return parent.resolve(configured).normalize();
            }
        }
        return cwd.resolve(configured).normalize();
    }

    // 将本地目录路径转换为 Spring 静态资源需要的目录 URI。
    static String toDirectoryLocation(Path root) {
        String location = root.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }

    // 拆分逗号分隔的配置项。
    private static String[] split(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toArray(String[]::new);
    }
}
