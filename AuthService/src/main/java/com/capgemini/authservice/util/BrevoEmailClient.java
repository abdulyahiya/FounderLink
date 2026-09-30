package com.capgemini.authservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class BrevoEmailClient {

    @Value("${brevo.api-key:${BREVO_API_KEY:}}")
    private String apiKey;

    @Value("${brevo.from-email:${BREVO_FROM:abdulyahya9973@gmail.com}}")
    private String defaultFromEmail;

    @Value("${brevo.from-name:${BREVO_NAME:FounderLink}}")
    private String defaultFromName;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean sendEmail(String toEmail, String subject, String htmlContent) {
        String key = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : System.getenv("BREVO_API_KEY");
        if (key == null || key.isBlank()) {
            return false;
        }

        String senderEmail = (defaultFromEmail != null && !defaultFromEmail.isBlank()) ? defaultFromEmail.trim() : "abdulyahya9973@gmail.com";
        String senderName = (defaultFromName != null && !defaultFromName.isBlank()) ? defaultFromName.trim() : "FounderLink";

        try {
            Map<String, Object> senderMap = new HashMap<>();
            senderMap.put("name", senderName);
            senderMap.put("email", senderEmail);

            Map<String, Object> toMap = new HashMap<>();
            toMap.put("email", toEmail.trim());

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", senderMap);
            payload.put("to", List.of(toMap));
            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", key)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[Brevo] Successfully sent email to {} via HTTPS (Status: {}, Body: {})", toEmail, response.statusCode(), response.body());
                return true;
            } else {
                log.error("[Brevo] Failed to send email to {}. Status: {}, Body: {}", toEmail, response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("[Brevo] Exception sending email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }
}
