package com.creatorspace.common.cache;

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
 * Redis JSON 短缓存封装，Redis 异常时按缓存未命中处理，业务继续走数据库。
 */
@Service
public class RedisJsonCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisJsonCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisJsonCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
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
            log.warn("读取 Redis 缓存失败: {}", key, exception);
            return Optional.empty();
        }
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
            log.warn("读取 Redis 缓存失败: {}", key, exception);
            return Optional.empty();
        }
    }

    public void write(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException | RuntimeException exception) {
            log.warn("写入 Redis 缓存失败: {}", key, exception);
        }
    }

    public boolean setIfAbsent(String key, Duration ttl) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", ttl));
        } catch (RuntimeException exception) {
            log.warn("写入 Redis 去重标记失败: {}", key, exception);
            throw exception;
        }
    }

    public void evict(String... keys) {
        try {
            redisTemplate.delete(Arrays.asList(keys));
        } catch (RuntimeException exception) {
            log.warn("清理 Redis 缓存失败: {}", Arrays.toString(keys), exception);
        }
    }
}