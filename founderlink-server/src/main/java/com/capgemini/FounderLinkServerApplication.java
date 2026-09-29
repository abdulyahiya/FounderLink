package com.capgemini;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication(scanBasePackages = "com.capgemini")
@EntityScan(basePackages = "com.capgemini")
@EnableJpaRepositories(
    basePackages = "com.capgemini",
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@EnableFeignClients(basePackages = "com.capgemini")
@EnableScheduling
public class FounderLinkServerApplication {

    public static void main(String[] args) {
        Map<String, Object> dbProps = configureDatabaseConnection();

        new SpringApplicationBuilder(FounderLinkServerApplication.class)
            .properties(dbProps)
            .beanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator())
            .run(args);
    }

    private static Map<String, Object> configureDatabaseConnection() {
        Map<String, Object> props = new HashMap<>();

        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getenv("SPRING_DATASOURCE_URL");
        }

        if (dbUrl != null && !dbUrl.isBlank()) {
            try {
                String clean = dbUrl.trim();
                if (clean.startsWith("jdbc:")) {
                    clean = clean.substring(5);
                }
                if (clean.startsWith("postgresql://") || clean.startsWith("postgres://")) {
                    clean = clean.replaceFirst("^postgres(ql)?://", "");
                }

                String username = null;
                String password = null;
                String hostPart = clean;

                if (clean.contains("@")) {
                    int atIdx = clean.indexOf("@");
                    String userInfo = clean.substring(0, atIdx);
                    hostPart = clean.substring(atIdx + 1);
                    if (userInfo.contains(":")) {
                        int colonIdx = userInfo.indexOf(":");
                        username = userInfo.substring(0, colonIdx);
                        password = userInfo.substring(colonIdx + 1);
                    } else {
                        username = userInfo;
                    }
                }

                String hostAndPort;
                String pathAndQuery = "founderlink_db?sslmode=require";
                if (hostPart.contains("/")) {
                    int slashIdx = hostPart.indexOf("/");
                    hostAndPort = hostPart.substring(0, slashIdx);
                    pathAndQuery = hostPart.substring(slashIdx + 1);
                } else {
                    hostAndPort = hostPart;
                }

                if (!hostAndPort.contains(":")) {
                    hostAndPort = hostAndPort + ":5432";
                }

                if (!pathAndQuery.contains("?")) {
                    pathAndQuery = pathAndQuery + "?sslmode=require";
                } else if (!pathAndQuery.contains("sslmode=")) {
                    pathAndQuery = pathAndQuery + "&sslmode=require";
                }

                String formattedJdbcUrl = "jdbc:postgresql://" + hostAndPort + "/" + pathAndQuery;

                props.put("spring.datasource.url", formattedJdbcUrl);
                System.setProperty("spring.datasource.url", formattedJdbcUrl);

                if (username != null) {
                    props.put("spring.datasource.username", username);
                    System.setProperty("spring.datasource.username", username);
                }
                if (password != null) {
                    props.put("spring.datasource.password", password);
                    System.setProperty("spring.datasource.password", password);
                }

                System.out.println("[Init] Converted database URL to JDBC: " + formattedJdbcUrl + " (user: " + (username != null ? username : "not-specified") + ")");
            } catch (Exception e) {
                System.err.println("[Init] Could not parse DATABASE_URL, using defaults: " + e.getMessage());
            }
        }
        return props;
    }
}
