package com.capgemini.team.feign;

import com.capgemini.team.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        log.error("[CIRCUIT BREAKER] user-service unavailable: {}", cause.getMessage());
        return id -> {
            throw new ServiceUnavailableException(
                    "User service is currently unavailable. Please try again later.");
        };
    }
}
