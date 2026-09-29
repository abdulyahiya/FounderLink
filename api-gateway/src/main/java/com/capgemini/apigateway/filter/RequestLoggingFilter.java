package com.capgemini.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Slf4j
@Order(1)
public class RequestLoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long startTime = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();

        log.info("[REQUEST] correlationId={} method={} path={}", correlationId, method, path);

        return chain.filter(exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-Correlation-Id", correlationId)
                        .build())
                .build())
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;
                    log.info("[RESPONSE] correlationId={} status={} duration={}ms",
                            correlationId, status, duration);
                });
    }
}
