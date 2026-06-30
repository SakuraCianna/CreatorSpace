package com.creatorspace.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisJsonCacheServiceTests {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final RedisJsonCacheService cacheService = new RedisJsonCacheService(redisTemplate, new ObjectMapper());

    @Test
    void readReturnsDeserializedValueWhenRedisHasJson() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache:key")).thenReturn("{\"name\":\"creator\",\"count\":3}");

        Optional<CachedSample> value = cacheService.read("cache:key", CachedSample.class);

        assertThat(value).contains(new CachedSample("creator", 3));
    }

    @Test
    void readReturnsEmptyWhenRedisFails() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        Optional<CachedSample> value = cacheService.read("cache:key", CachedSample.class);

        assertThat(value).isEmpty();
    }

    @Test
    void writeSerializesValueWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.write("cache:key", new CachedSample("creator", 3), Duration.ofMinutes(2));

        verify(valueOperations).set(eq("cache:key"), eq("{\"name\":\"creator\",\"count\":3}"), eq(Duration.ofMinutes(2)));
    }

    @Test
    void setIfAbsentReturnsRedisResultAndPropagatesFailureForFallbackCaller() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("dedupe:key", "1", Duration.ofMinutes(5))).thenReturn(true);

        assertThat(cacheService.setIfAbsent("dedupe:key", Duration.ofMinutes(5))).isTrue();

        doThrow(new RuntimeException("redis down"))
                .when(valueOperations)
                .setIfAbsent("dedupe:key", "1", Duration.ofMinutes(5));
        assertThatCode(() -> cacheService.setIfAbsent("dedupe:key", Duration.ofMinutes(5)))
                .isInstanceOf(RuntimeException.class);
    }

    record CachedSample(String name, int count) {
    }
}