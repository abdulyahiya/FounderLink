package com.capgemini.payment.controller;

import com.capgemini.payment.dto.CreateOrderRequest;
import com.capgemini.payment.dto.VerifyPaymentRequest;
import com.capgemini.payment.entity.Payment;
import com.capgemini.payment.saga.PaymentSaga;
import com.capgemini.payment.saga.SagaOrchestrator;
import com.capgemini.payment.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            Map<String, Object> response = paymentService.createOrder(request);
            return ResponseEntity.ok(response);
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
    public ResponseEntity<Map<String, Object>> acceptPayment(@PathVariable Long paymentId) {
        try {
            return ResponseEntity.ok(paymentService.acceptPayment(paymentId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{paymentId}/reject")
    public ResponseEntity<Map<String, Object>> rejectPayment(@PathVariable Long paymentId) {
        try {
            return ResponseEntity.ok(paymentService.rejectPayment(paymentId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/investor/{investorId}")
    public ResponseEntity<List<Payment>> getPaymentsByInvestor(@PathVariable Long investorId) {
        return ResponseEntity.ok(paymentService.getPaymentsByInvestor(investorId));
    }

    @GetMapping("/founder/{founderId}")
    public ResponseEntity<List<Payment>> getPaymentsByFounder(@PathVariable Long founderId) {
        return ResponseEntity.ok(paymentService.getPaymentsByFounder(founderId));
    }

    @GetMapping("/startup/{startupId}")
    public ResponseEntity<List<Payment>> getPaymentsByStartup(@PathVariable Long startupId) {
        return ResponseEntity.ok(paymentService.getPaymentsByStartup(startupId));
    }

    // Returns current saga state for a payment — useful for debugging/monitoring
    @GetMapping("/{paymentId}/saga")
    public ResponseEntity<PaymentSaga> getSagaStatus(@PathVariable Long paymentId) {
        try {
            return ResponseEntity.ok(sagaOrchestrator.getSagaByPaymentId(paymentId));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
