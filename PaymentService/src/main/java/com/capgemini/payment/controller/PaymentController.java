package com.capgemini.payment.controller;

import com.capgemini.payment.dto.CreateOrderRequest;
import com.capgemini.payment.dto.VerifyPaymentRequest;
import com.capgemini.payment.entity.Payment;
import com.capgemini.payment.saga.PaymentSaga;
import com.capgemini.payment.saga.SagaOrchestrator;
import com.capgemini.payment.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:5173"})
public class PaymentController {

    private final IPaymentService paymentService;
    private final SagaOrchestrator sagaOrchestrator;

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String headerRoles,
            Authentication authentication) {
        try {
            Long callerUserId = resolveUserId(authentication, headerUserId);
            boolean isAdmin = resolveIsAdmin(authentication, headerRoles);
            Map<String, Object> response = paymentService.createOrder(request, callerUserId, isAdmin);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return handleAccessDenied(e);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody VerifyPaymentRequest request) {
        Map<String, Object> response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{paymentId}/accept")
    public ResponseEntity<Map<String, Object>> acceptPayment(
            @PathVariable Long paymentId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String headerRoles,
            Authentication authentication) {
        try {
            Long callerUserId = resolveUserId(authentication, headerUserId);
            boolean isAdmin = resolveIsAdmin(authentication, headerRoles);
            return ResponseEntity.ok(paymentService.acceptPayment(paymentId, callerUserId, isAdmin));
        } catch (AccessDeniedException e) {
            return handleAccessDenied(e);
        } catch (IllegalStateException e) {
            return handleIllegalState(e);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{paymentId}/reject")
    public ResponseEntity<Map<String, Object>> rejectPayment(
            @PathVariable Long paymentId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String headerRoles,
            Authentication authentication) {
        try {
            Long callerUserId = resolveUserId(authentication, headerUserId);
            boolean isAdmin = resolveIsAdmin(authentication, headerRoles);
            return ResponseEntity.ok(paymentService.rejectPayment(paymentId, callerUserId, isAdmin));
        } catch (AccessDeniedException e) {
            return handleAccessDenied(e);
        } catch (IllegalStateException e) {
            return handleIllegalState(e);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/investor/{investorId}")
    public ResponseEntity<?> getPaymentsByInvestor(
            @PathVariable Long investorId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String headerRoles,
            Authentication authentication) {
        try {
            Long callerUserId = resolveUserId(authentication, headerUserId);
            boolean isAdmin = resolveIsAdmin(authentication, headerRoles);
            return ResponseEntity.ok(paymentService.getPaymentsByInvestor(investorId, callerUserId, isAdmin));
        } catch (AccessDeniedException e) {
            return handleAccessDenied(e);
        }
    }

    @GetMapping("/founder/{founderId}")
    public ResponseEntity<?> getPaymentsByFounder(
            @PathVariable Long founderId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String headerRoles,
            Authentication authentication) {
        try {
            Long callerUserId = resolveUserId(authentication, headerUserId);
            boolean isAdmin = resolveIsAdmin(authentication, headerRoles);
            return ResponseEntity.ok(paymentService.getPaymentsByFounder(founderId, callerUserId, isAdmin));
        } catch (AccessDeniedException e) {
            return handleAccessDenied(e);
        }
    }

    @GetMapping("/startup/{startupId}")
    public ResponseEntity<?> getPaymentsByStartup(
            @PathVariable Long startupId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String headerRoles,
            Authentication authentication) {
        try {
            Long callerUserId = resolveUserId(authentication, headerUserId);
            boolean isAdmin = resolveIsAdmin(authentication, headerRoles);
            return ResponseEntity.ok(paymentService.getPaymentsByStartup(startupId, callerUserId, isAdmin));
        } catch (AccessDeniedException e) {
            return handleAccessDenied(e);
        }
    }

    // Returns current saga state for a payment — authorized to participants and admins
    @GetMapping("/{paymentId}/saga")
    public ResponseEntity<?> getSagaStatus(
            @PathVariable Long paymentId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String headerRoles,
            Authentication authentication) {
        try {
            Long callerUserId = resolveUserId(authentication, headerUserId);
            boolean isAdmin = resolveIsAdmin(authentication, headerRoles);
            return ResponseEntity.ok(paymentService.getSagaStatus(paymentId, callerUserId, isAdmin));
        } catch (AccessDeniedException e) {
            return handleAccessDenied(e);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", ex.getMessage());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", ex.getMessage());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private Long resolveUserId(Authentication authentication, Long headerUserId) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long l) {
                return l;
            }
            if (principal instanceof Number n) {
                return n.longValue();
            }
            String name = authentication.getName();
            if (name != null && !name.isBlank()) {
                try {
                    return Long.parseLong(name.trim());
                } catch (NumberFormatException ignored) {}
            }
            if (principal != null) {
                try {
                    return Long.parseLong(principal.toString().trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        if (headerUserId != null) {
            return headerUserId;
        }
        throw new AccessDeniedException("Authentication required");
    }

    private boolean resolveIsAdmin(Authentication authentication, String headerRoles) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            if (authentication.getAuthorities() != null) {
                return authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a) || "ADMIN".equalsIgnoreCase(a));
            }
        }
        return false;
    }
}
