package com.suresh.sms.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Cache entries expire after 10 minutes and a cache miss is never itself
 * cached (a {@code null} would otherwise mask a student being created
 * moments later). Active only when {@code spring.cache.type=redis} (the
 * default); the test profile sets it to {@code none}, so tests exercise the
 * real database on every call without needing Redis.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    /**
     * Caching is a performance optimization, not a hard dependency: if
     * Redis is slow, unreachable, or simply not running (for example when
     * the app is started with {@code mvn spring-boot:run} instead of
     * {@code docker compose}, which is the only place Redis is defined),
     * a request should still succeed by falling through to the database -
     * not fail with a 500 just because the cache layer had a problem.
     */
    @Bean
    public CacheErrorHandler cacheErrorHandler() {

        return new CacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Cache read failed for '{}' (key={}); falling back to the "
                        + "database: {}", cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCachePutError(
                    RuntimeException ex, Cache cache, Object key, Object value) {
                log.warn("Cache write failed for '{}' (key={}): {}",
                        cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Cache evict failed for '{}' (key={}): {}",
                        cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.warn("Cache clear failed for '{}': {}", cache.getName(), ex.getMessage());
            }
        };
    }

    // Only defines the Redis-specific TTL/serialization when Redis caching
    // is actually active; with spring.cache.type=none (the test profile),
    // Spring falls back to a no-op cache manager and this bean is unused.
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheConfiguration cacheConfiguration() {

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()));
    }
}
