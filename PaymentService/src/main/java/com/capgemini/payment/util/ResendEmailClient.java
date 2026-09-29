package com.capgemini.payment.util;

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
public class ResendEmailClient {

    @Value("${resend.api-key:${RESEND_API_KEY:}}")
    private String apiKey;

    @Value("${resend.from-email:${RESEND_FROM:FounderLink <onboarding@resend.dev>}}")
    private String defaultFromEmail;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean sendEmail(String toEmail, String subject, String htmlBody) {
        return sendEmail(defaultFromEmail, toEmail, subject, htmlBody);
    }

    public boolean sendEmail(String from, String toEmail, String subject, String htmlBody) {
        String key = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : System.getenv("RESEND_API_KEY");
        if (key == null || key.isBlank()) {
            log.debug("[Resend] No RESEND_API_KEY configured, skipping Resend HTTP send");
            return false;
        }

        String sender = (from != null && !from.isBlank()) ? from.trim() : "FounderLink <onboarding@resend.dev>";
        if (!sender.contains("<")) {
            sender = "FounderLink <" + sender + ">";
        }
        if (sender.contains("gmail.com")) {
            sender = "FounderLink <onboarding@resend.dev>";
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("from", sender);
            payload.put("to", List.of(toEmail.trim()));
            payload.put("subject", subject);
            payload.put("html", htmlBody);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[Resend] Successfully sent payment email to {} via HTTPS (Status: {}, Body: {})", toEmail, response.statusCode(), response.body());
                return true;
            } else {
                log.error("[Resend] Failed to send payment email to {}. Status: {}, Body: {}", toEmail, response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("[Resend] Exception sending payment email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }
}
