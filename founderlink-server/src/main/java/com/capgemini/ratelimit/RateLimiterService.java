package com.capgemini.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> rateLimitScript;

    // Observability metrics for Redis health and fail-open alerting
    private final AtomicLong totalFailures = new AtomicLong(0);
    private final AtomicLong consecutiveFailures = new AtomicLong(0);

    // Lua script executing atomic INCR and insured EXPIRE, returning {current_count, ttl}
    // Guarantees key expiration even if a previous process was interrupted or key was set without TTL
    private static final String LUA_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            local ttl = redis.call('TTL', KEYS[1])
            if ttl == -1 or current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
                ttl = tonumber(ARGV[1])
            end
            return {current, ttl}
            """;

    @Autowired
    public RateLimiterService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>(LUA_SCRIPT, List.class);
    }

    public RateLimitResult checkLimit(RateLimitPolicy policy, String clientIp) {
        if (redisTemplate == null) {
            log.warn("[RateLimiter] StringRedisTemplate not configured; failing open for clientIp={}", clientIp);
            return RateLimitResult.builder()
                    .allowed(true)
                    .currentCount(0)
                    .limit(policy.getMaxRequests())
                    .retryAfterSeconds(0)
                    .build();
        }

        String key = "ratelimit:" + policy.getKeyPrefix() + ":" + clientIp;
        try {
            List<?> result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(policy.getWindowSeconds())
            );

            if (result != null && result.size() >= 2) {
                long currentCount = ((Number) result.get(0)).longValue();
                long ttl = ((Number) result.get(1)).longValue();

                // On success, recover any consecutive failure count
                if (consecutiveFailures.get() > 0) {
                    log.info("[RateLimiter] Redis connection recovered after {} consecutive failures.", consecutiveFailures.getAndSet(0));
                }

                boolean allowed = currentCount <= policy.getMaxRequests();
                long retryAfter = allowed ? 0 : Math.max(1, ttl);

                if (!allowed) {
                    log.warn("[RateLimiter] Limit exceeded for key={} (count={}/{}, retryAfter={}s)",
                            key, currentCount, policy.getMaxRequests(), retryAfter);
                }

                return RateLimitResult.builder()
                        .allowed(allowed)
                        .currentCount(currentCount)
                        .limit(policy.getMaxRequests())
                        .retryAfterSeconds(retryAfter)
                        .build();
            }
        } catch (Exception e) {
            long total = totalFailures.incrementAndGet();
            long consecutive = consecutiveFailures.incrementAndGet();

            // Fail-open: do not lock out legitimate users during transient Redis outages,
            // but emit critical observability logs so operations are immediately alerted.
            if (consecutive == 1 || consecutive % 50 == 0) {
                log.error("[RateLimiter] [CRITICAL_OBSERVABILITY] Redis rate limiter failure #{} (consecutive={}). System is failing open and running without distributed rate limiting: {}",
                        total, consecutive, e.getMessage());
            }
        }

        return RateLimitResult.builder()
                .allowed(true)
                .currentCount(0)
                .limit(policy.getMaxRequests())
                .retryAfterSeconds(0)
                .build();
    }

    public long getTotalFailures() {
        return totalFailures.get();
    }

    public long getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    public boolean isRedisHealthy() {
        return redisTemplate != null && consecutiveFailures.get() == 0;
    }
}
