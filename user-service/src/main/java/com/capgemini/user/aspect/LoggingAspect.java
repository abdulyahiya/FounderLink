package com.capgemini.user.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.capgemini.user.service..*(..))")
    public Object logServiceMethods(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();
        log.info("[START] {}", method);
        try {
            Object result = pjp.proceed();
            log.info("[END] {} | {}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("[ERROR] {} | {}ms | {}: {}",
                    method, System.currentTimeMillis() - start,
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }
}
