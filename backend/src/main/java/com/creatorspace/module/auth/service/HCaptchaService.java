package com.creatorspace.module.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class HCaptchaService {

    private static final Logger log = LoggerFactory.getLogger(HCaptchaService.class);
    private static final String VERIFY_URL = "https://api.hcaptcha.com/siteverify";

    private final String secretKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HCaptchaService(
            @Value("${app.security.hcaptcha-secret-key:}") String secretKey,
            ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 验证 hCaptcha token
     *
     * @param token 前端传来的 hcaptchaToken
     * @return 如果验证通过或者未配置密钥，返回 true；否则返回 false
     */
    public boolean verify(String token) {
        if (!StringUtils.hasText(secretKey)) {
            log.warn("HCAPTCHA_SECRET_KEY is not configured, skipping verification.");
            return true; // 如果未配置密钥，默认放行方便本地调试
        }

        if (!StringUtils.hasText(token)) {
            log.warn("Empty hcaptcha token provided.");
            return false;
        }

        try {
            String formData = "secret=" + secretKey + "&response=" + token;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERIFY_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                boolean success = jsonNode.path("success").asBoolean();
                if (!success) {
                    log.warn("hCaptcha verification failed. Response: {}", response.body());
                }
                return success;
            } else {
                log.error("hCaptcha verification HTTP error: {}", response.statusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Error communicating with hCaptcha API", e);
            return false;
        }
    }
}
