package com.capgemini;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.capgemini")
@EntityScan(basePackages = "com.capgemini")
@EnableJpaRepositories(basePackages = "com.capgemini")
@EnableScheduling
public class FounderLinkServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FounderLinkServerApplication.class, args);
    }
}
