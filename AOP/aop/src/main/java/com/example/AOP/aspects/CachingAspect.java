package com.example.AOP.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import com.example.AOP.Annotation.Cacheable;

@Component
@Aspect
public class CachingAspect {

    private static final Logger logger = LoggerFactory.getLogger(CachingAspect.class);
    private final Cache<String, CacheEntry> cache = Caffeine.newBuilder()
            .maximumSize(10000)
            .build();

    /**
     * Around Advice - Caching
     */
    @Around("@annotation(cacheable)")
    public Object cache(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        String cacheKey = generateCacheKey(joinPoint, cacheable);
        long cacheDuration = cacheable.duration() * 1000;
        
        CacheEntry entry = cache.getIfPresent(cacheKey);
        if (entry != null) {
            if (System.currentTimeMillis() - entry.timestamp < cacheDuration) {
                logger.info("من الـ Cache: {} (العمر: {} ms)",
                        cacheKey, System.currentTimeMillis() - entry.timestamp);
                return entry.value;
            } else {
                cache.invalidate(cacheKey);
            }
        }

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;

        cache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis()));
        logger.info("تم الـ Caching: {} ({} ms)", cacheKey, duration);

        return result;
    }

    private String generateCacheKey(ProceedingJoinPoint joinPoint, Cacheable cacheable) {
        if (cacheable.key() != null && !cacheable.key().isEmpty()) {
            return cacheable.key();
        }
        return joinPoint.getSignature().getName();
    }

    private static class CacheEntry {
        Object value;
        long timestamp;

        CacheEntry(Object value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}