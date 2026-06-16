package com.creatorspace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Value("${app.storage.local-root:storage/uploads}")
    private String localStorageRoot;

    @Value("${app.storage.public-prefix:/uploads}")
    private String publicPrefix;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(split(allowedOrigins))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = publicPrefix.startsWith("/") ? publicPrefix : "/" + publicPrefix;
        Path root = resolveFromProjectRoot(localStorageRoot);
        registry.addResourceHandler(prefix + "/**")
                .addResourceLocations(root.toUri().toString());
    }

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

    private static String[] split(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toArray(String[]::new);
    }
}
