package com.capgemini;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.capgemini")
@EntityScan(basePackages = "com.capgemini")
@EnableJpaRepositories(
    basePackages = "com.capgemini",
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@EnableScheduling
public class FounderLinkServerApplication {

    public static void main(String[] args) {
        // Automatically sanitize Render's DATABASE_URL into standard Java JDBC format
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl != null && !dbUrl.startsWith("jdbc:")) {
            System.setProperty("spring.datasource.url", "jdbc:" + dbUrl);
        }

        // Also normalize SPRING_DATASOURCE_URL if present
        String springDbUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (springDbUrl != null && !springDbUrl.startsWith("jdbc:")) {
            System.setProperty("spring.datasource.url", "jdbc:" + springDbUrl);
        }

        new SpringApplicationBuilder(FounderLinkServerApplication.class)
            .beanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator())
            .run(args);
    }
}
