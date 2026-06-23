package com.creatorspace.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTests {

    @Test
    void localEnvironmentAllowsPlaceholderJwtSecret() {
        JwtService jwtService = new JwtService("short", 120, "local");

        String token = jwtService.createAccessToken(new LoginUser(1L, "admin", List.of("ADMIN")));
        LoginUser parsedUser = jwtService.parseAccessToken(token);

        assertThat(parsedUser.userId()).isEqualTo(1L);
        assertThat(parsedUser.username()).isEqualTo("admin");
        assertThat(parsedUser.roles()).containsExactly("ADMIN");
    }

    @Test
    void nonLocalEnvironmentRejectsWeakJwtSecret() {
        assertThatThrownBy(() -> new JwtService("short", 120, "production"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be configured with at least 32 characters outside local development");
    }

    @Test
    void blankEnvironmentRejectsWeakJwtSecret() {
        assertThatThrownBy(() -> new JwtService("short", 120, " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be configured with at least 32 characters outside local development");
    }
}
