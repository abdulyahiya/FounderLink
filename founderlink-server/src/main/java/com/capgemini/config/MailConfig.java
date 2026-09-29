package com.capgemini.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@Slf4j
public class MailConfig {

    @Value("${spring.mail.host:${MAIL_HOST:smtp.gmail.com}}")
    private String host;

    @Value("${spring.mail.port:${MAIL_PORT:465}}")
    private int port;

    @Value("${spring.mail.username:${MAIL_USERNAME:abdulyahya9973@gmail.com}}")
    private String username;

    @Value("${spring.mail.password:${MAIL_PASSWORD:xooecxywzeuahpng}}")
    private String password;

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        String resolvedHost = (host != null && !host.isBlank()) ? host.trim() : "smtp.gmail.com";
        mailSender.setHost(resolvedHost);

        int resolvedPort = (port > 0) ? port : 465;
        // Cloud providers like Render block outbound port 587; auto-switch to 465 SSL
        if (resolvedPort == 587) {
            resolvedPort = 465;
        }
        mailSender.setPort(resolvedPort);

        String cleanUsername = username != null ? username.trim() : "abdulyahya9973@gmail.com";
        String cleanPassword = password != null ? password.trim().replaceAll("\\s+", "") : "xooecxywzeuahpng";

        mailSender.setUsername(cleanUsername);
        mailSender.setPassword(cleanPassword);
        mailSender.setDefaultEncoding("UTF-8");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtps");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.smtp.from", cleanUsername);

        props.put("mail.debug", "true");

        log.info("[MailConfig] Initialized JavaMailSender: host={}, port={}, username={}",
                resolvedHost, resolvedPort, cleanUsername);

        return mailSender;
    }
}
