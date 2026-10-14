package com.capgemini.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(redisTemplate);
    }

    @Test
    void checkLimit_whenWithinLimit_shouldAllow() {
        // given: 3rd request out of 5 allowed
        given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .willReturn(List.of(3L, 50L));

        // when
        RateLimitResult result = rateLimiterService.checkLimit(RateLimitPolicy.LOGIN, "192.168.1.1");

        // then
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getCurrentCount()).isEqualTo(3L);
        assertThat(result.getRetryAfterSeconds()).isEqualTo(0L);
        assertThat(rateLimiterService.isRedisHealthy()).isTrue();
    }

    @Test
    void checkLimit_whenLimitExceeded_shouldBlockAndReturnPositiveRetryAfter() {
        // given: 6th request out of 5 allowed, 45 seconds TTL remaining
        given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .willReturn(List.of(6L, 45L));

        // when
        RateLimitResult result = rateLimiterService.checkLimit(RateLimitPolicy.LOGIN, "192.168.1.1");

        // then
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getCurrentCount()).isEqualTo(6L);
        assertThat(result.getRetryAfterSeconds()).isEqualTo(45L);
    }

    @Test
    void checkLimit_whenLimitExceededAndTtlZero_shouldEnforceMinimumRetryAfterOfOneSecond() {
        // given: 6th request out of 5 allowed, but TTL returns 0
        given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .willReturn(List.of(6L, 0L));

        // when
        RateLimitResult result = rateLimiterService.checkLimit(RateLimitPolicy.LOGIN, "192.168.1.1");

        // then: Retry-After must be positive (>= 1)
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRetryAfterSeconds()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void checkLimit_whenRedisThrowsException_shouldFailOpenAndTrackObservabilityMetrics() {
        // given: Redis connection failure
        given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .willThrow(new RedisConnectionFailureException("Connection refused to Upstash Redis"));

        // when
        RateLimitResult result = rateLimiterService.checkLimit(RateLimitPolicy.LOGIN, "192.168.1.1");

        // then: fail-open ensures legitimate users are not locked out during Redis outage
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getCurrentCount()).isEqualTo(0L);
        assertThat(rateLimiterService.getTotalFailures()).isEqualTo(1L);
        assertThat(rateLimiterService.getConsecutiveFailures()).isEqualTo(1L);
        assertThat(rateLimiterService.isRedisHealthy()).isFalse();
    }

    @Test
    void checkLimit_whenRedisRecoversAfterFailure_shouldResetConsecutiveFailures() {
        // given: 1 failure followed by 1 success
        given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .willThrow(new RedisConnectionFailureException("Connection error"))
                .willReturn(List.of(1L, 60L));

        // Call 1 fails
        rateLimiterService.checkLimit(RateLimitPolicy.LOGIN, "192.168.1.1");
        assertThat(rateLimiterService.getConsecutiveFailures()).isEqualTo(1L);

        // Call 2 succeeds
        RateLimitResult recoveredResult = rateLimiterService.checkLimit(RateLimitPolicy.LOGIN, "192.168.1.1");
        assertThat(recoveredResult.isAllowed()).isTrue();
        assertThat(rateLimiterService.getConsecutiveFailures()).isEqualTo(0L);
        assertThat(rateLimiterService.isRedisHealthy()).isTrue();
    }

    @Test
    void checkLimit_whenRedisTemplateNull_shouldFailOpen() {
        // given: service created with null template
        RateLimiterService nullRedisService = new RateLimiterService(null);

        // when
        RateLimitResult result = nullRedisService.checkLimit(RateLimitPolicy.LOGIN, "192.168.1.1");

        // then
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getCurrentCount()).isEqualTo(0L);
        assertThat(nullRedisService.isRedisHealthy()).isFalse();
    }

    @Test
    void checkLimit_underConcurrentAccess_shouldHandleCorrectly() throws InterruptedException {
        // Simulate concurrent requests
        AtomicInteger counter = new AtomicInteger(0);
        given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .willAnswer(inv -> List.of((long) counter.incrementAndGet(), 60L));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    RateLimitResult res = rateLimiterService.checkLimit(RateLimitPolicy.LOGIN, "10.0.0.1");
                    if (res.isAllowed()) {
                        allowedCount.incrementAndGet();
                    } else {
                        blockedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 5 should be allowed (limit is 5), and 5 blocked
        assertThat(allowedCount.get()).isEqualTo(5);
        assertThat(blockedCount.get()).isEqualTo(5);
    }
}
