package com.capgemini.team.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "startup-service", configuration = FeignClientConfig.class,
        fallbackFactory = StartupClientFallbackFactory.class)
public interface StartupClient {

    @GetMapping("/startups/{id}")
    StartupDTO getStartupById(@PathVariable("id") Long id);
}
