package com.capgemini.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
@Slf4j
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip CORS pre-flight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        String path = uri.toLowerCase();

        // Skip health checks, swagger documentation, and static paths
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.endsWith(".ico")
                || path.endsWith(".html")
                || path.endsWith(".js")
                || path.endsWith(".css")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".svg")
                || path.endsWith(".woff")
                || path.endsWith(".woff2");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = clientIpResolver.resolveClientIp(request);
        RateLimitPolicy policy = RateLimitPolicy.resolve(request.getRequestURI(), request.getMethod());

        RateLimitResult result = rateLimiterService.checkLimit(policy, clientIp);

        if (!result.isAllowed()) {
            long retryAfter = Math.max(1, result.getRetryAfterSeconds());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(retryAfter));

            String json = String.format(
                    "{\"status\":429,\"message\":\"Too many requests. Please try again after %d seconds.\",\"data\":null}",
                    retryAfter
            );

            response.getWriter().write(json);
            response.getWriter().flush();
            return;
        }

        filterChain.doFilter(request, response);
    }
}
