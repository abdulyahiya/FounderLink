package com.capgemini.investment.controller;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test/token")
public class DemoTokenController {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @GetMapping("/investor")
    public ResponseEntity<Map<String, String>> getInvestorToken() {
        String token = generateToken(1L, "investor@test.com", List.of("INVESTOR"));
        return ResponseEntity.ok(Map.of("token", token, "role", "INVESTOR", "userId", "1"));
    }

    @GetMapping("/founder")
    public ResponseEntity<Map<String, String>> getFounderToken() {
        String token = generateToken(2L, "founder@test.com", List.of("FOUNDER"));
        return ResponseEntity.ok(Map.of("token", token, "role", "FOUNDER", "userId", "2"));
    }

    @GetMapping("/cofounder")
    public ResponseEntity<Map<String, String>> getCofounderToken() {
        String token = generateToken(3L, "cofounder@test.com", List.of("COFOUNDER"));
        return ResponseEntity.ok(Map.of("token", token, "role", "COFOUNDER", "userId", "3"));
    }

    @GetMapping("/admin")
    public ResponseEntity<Map<String, String>> getAdminToken() {
        String token = generateToken(4L, "admin@test.com", List.of("ADMIN"));
        return ResponseEntity.ok(Map.of("token", token, "role", "ADMIN", "userId", "4"));
    }

    private String generateToken(Long userId, String email, List<String> roles) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }
}
