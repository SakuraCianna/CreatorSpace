package com.creatorspace.config;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 后台接口认证和授权配置。
 */
@Configuration
public class SecurityConfig {

    // 配置无状态 JWT 安全链路和公开接口白名单。
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper
    )
            throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(accessDeniedHandler(objectMapper))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/actuator/health").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/admin/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/**", "/api/projects/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/inspirations/**", "/api/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/site/config", "/api/site/statistics/summary", "/api/theme/current", "/api/themes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/tags").permitAll()
                        .requestMatchers("/api/creator/**", "/api/me/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // 创建 BCrypt 密码编码器。
    @Bean
    PasswordEncoder passwordEncoder(Environment environment) {
        int strength = environment.getProperty("app.security.password-bcrypt-strength", Integer.class, 12);
        return new BCryptPasswordEncoder(strength);
    }

    // 创建未认证请求的统一 JSON 入口点，避免浏览器收到 Basic challenge 后弹原生登录框。
    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(
                    response.getWriter(),
                    ApiResponse.fail("请先登录后再访问")
            );
        };
    }

    // 创建已登录但权限不足请求的统一 JSON 响应，避免前端只能看到空白 403。
    @Bean
    AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(
                    response.getWriter(),
                    ApiResponse.fail("当前账号没有后台权限或登录状态不匹配")
            );
        };
    }
}
