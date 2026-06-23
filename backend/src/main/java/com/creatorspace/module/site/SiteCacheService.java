package com.creatorspace.module.site;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * 公开站点配置缓存，后台更新后主动失效，Redis 不可用时回退到数据库读取。
 */
@Service
public class SiteCacheService {

    private static final Logger log = LoggerFactory.getLogger(SiteCacheService.class);

    public static final String SITE_CONFIG_KEY = "creatorspace:site:config";
    public static final String CURRENT_THEME_KEY = "creatorspace:theme:current";
    public static final String THEME_LIST_KEY = "creatorspace:theme:list";

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SiteCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> read(String key, TypeReference<T> typeReference) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(raw, typeReference));
        } catch (JsonProcessingException exception) {
            evict(key);
            return Optional.empty();
        } catch (RuntimeException exception) {
            log.warn("读取站点缓存失败: {}", key, exception);
            return Optional.empty();
        }
    }

    public <T> Optional<T> read(String key, Class<T> type) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(raw, type));
        } catch (JsonProcessingException exception) {
            evict(key);
            return Optional.empty();
        } catch (RuntimeException exception) {
            log.warn("读取站点缓存失败: {}", key, exception);
            return Optional.empty();
        }
    }

    public void write(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), CACHE_TTL);
        } catch (JsonProcessingException | RuntimeException exception) {
            log.warn("写入站点缓存失败: {}", key, exception);
        }
    }

    public void evictSiteConfig() {
        evict(SITE_CONFIG_KEY);
    }

    public void evictThemes() {
        evict(CURRENT_THEME_KEY, THEME_LIST_KEY);
    }

    public void evict(String... keys) {
        try {
            redisTemplate.delete(Arrays.asList(keys));
        } catch (RuntimeException exception) {
            log.warn("清理站点缓存失败: {}", Arrays.toString(keys), exception);
        }
    }
}
