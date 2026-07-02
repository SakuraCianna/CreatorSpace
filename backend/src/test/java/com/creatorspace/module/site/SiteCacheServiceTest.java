package com.creatorspace.module.site;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SiteCacheServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final SiteCacheService service = new SiteCacheService(redisTemplate, new ObjectMapper());

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void readSupportsClassAndTypeReferenceAndEvictsInvalidJson() {
        when(valueOperations.get("profile")).thenReturn("{\"name\":\"Creator\"}");
        Map<?, ?> profile = service.read("profile", Map.class).orElseThrow();
        assertEquals("Creator", profile.get("name"));

        when(valueOperations.get("list")).thenReturn("[\"a\",\"b\"]");
        List<String> values = service.read("list", new TypeReference<List<String>>() {
        }).orElseThrow();
        assertEquals(List.of("a", "b"), values);

        when(valueOperations.get("broken")).thenReturn("{bad-json");
        assertTrue(service.read("broken", Map.class).isEmpty());
        verify(redisTemplate).delete(List.of("broken"));
    }

    @Test
    void readReturnsEmptyForMissingOrRedisFailure() {
        when(valueOperations.get("missing")).thenReturn(null);
        assertTrue(service.read("missing", Map.class).isEmpty());

        when(valueOperations.get("down")).thenThrow(new RuntimeException("redis down"));
        assertTrue(service.read("down", new TypeReference<Map<String, Object>>() {
        }).isEmpty());
    }

    @Test
    void writeAndEvictUseExpectedKeysAndIgnoreFailures() {
        service.write("site", Map.of("enabled", true));
        verify(valueOperations).set(eq("site"), contains("\"enabled\":true"), eq(Duration.ofMinutes(10)));

        doThrow(new RuntimeException("write down")).when(valueOperations).set(eq("bad"), anyString(), any(Duration.class));
        assertDoesNotThrow(() -> service.write("bad", Map.of("enabled", false)));

        service.evictSiteConfig();
        service.evictThemes();
        verify(redisTemplate).delete(List.of(SiteCacheService.SITE_CONFIG_KEY));
        verify(redisTemplate).delete(List.of(SiteCacheService.CURRENT_THEME_KEY, SiteCacheService.THEME_LIST_KEY));

        doThrow(new RuntimeException("delete down")).when(redisTemplate).delete(List.of("x"));
        assertDoesNotThrow(() -> service.evict("x"));
    }
}
