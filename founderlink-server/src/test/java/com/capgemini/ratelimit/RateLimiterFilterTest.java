package com.capgemini.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimiterFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private ClientIpResolver clientIpResolver;

    @Mock
    private FilterChain filterChain;

    private RateLimiterFilter rateLimiterFilter;

    @BeforeEach
    void setUp() {
        rateLimiterFilter = new RateLimiterFilter(rateLimiterService, clientIpResolver);
    }

    @Test
    void doFilter_whenAllowed_shouldProceedChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(clientIpResolver.resolveClientIp(request)).willReturn("192.168.1.50");
        given(rateLimiterService.checkLimit(eq(RateLimitPolicy.LOGIN), eq("192.168.1.50")))
                .willReturn(RateLimitResult.builder().allowed(true).currentCount(1).limit(5).retryAfterSeconds(0).build());

        rateLimiterFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_whenBlocked_shouldReturn429WithRetryAfter() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(clientIpResolver.resolveClientIp(request)).willReturn("192.168.1.50");
        given(rateLimiterService.checkLimit(eq(RateLimitPolicy.LOGIN), eq("192.168.1.50")))
                .willReturn(RateLimitResult.builder().allowed(false).currentCount(6).limit(5).retryAfterSeconds(42).build());

        rateLimiterFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("42");
        assertThat(response.getContentAsString()).contains("\"status\":429");
        assertThat(response.getContentAsString()).contains("Too many requests");

        // Verify downstream filters and controllers are NOT called
        verify(filterChain, never()).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    void doFilter_whenCorsOptionsPreflight_shouldBypassWithoutConsumingLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimiterFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).checkLimit(any(), any());
    }

    @Test
    void doFilter_whenActuatorHealthCheck_shouldBypass() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimiterFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).checkLimit(any(), any());
    }

    @Test
    void doFilter_whenStaticAssetOrSwagger_shouldBypass() throws ServletException, IOException {
        MockHttpServletRequest requestJs = new MockHttpServletRequest("GET", "/static/main.js");
        MockHttpServletResponse responseJs = new MockHttpServletResponse();
        rateLimiterFilter.doFilter(requestJs, responseJs, filterChain);

        MockHttpServletRequest requestSwagger = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse responseSwagger = new MockHttpServletResponse();
        rateLimiterFilter.doFilter(requestSwagger, responseSwagger, filterChain);

        verify(rateLimiterService, never()).checkLimit(any(), any());
    }
}
