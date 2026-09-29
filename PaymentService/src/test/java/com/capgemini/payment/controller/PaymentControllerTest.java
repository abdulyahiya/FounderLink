package com.capgemini.payment.controller;

import com.capgemini.payment.dto.CreateOrderRequest;
import com.capgemini.payment.dto.VerifyPaymentRequest;
import com.capgemini.payment.entity.Payment;
import com.capgemini.payment.saga.SagaOrchestrator;
import com.capgemini.payment.service.IPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.RazorpayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private IPaymentService paymentService;

    @MockBean
    private SagaOrchestrator sagaOrchestrator;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── POST /api/payments/create-order ─────────────────────────────────────

    @Test
    void createOrder_success_returns200() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setInvestorId(1L);
        request.setFounderId(2L);
        request.setStartupId(3L);
        request.setStartupName("StartupX");
        request.setInvestorName("InvestorY");
        request.setAmount(5000.0);

        Map<String, Object> response = Map.of(
                "orderId", "order_123",
                "amount", 500000,
                "currency", "INR",
                "keyId", "test_key_id"
        );
        when(paymentService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order_123"))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void createOrder_serviceThrowsException_returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(5000.0);

        when(paymentService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new RazorpayException("Razorpay error"));

        mockMvc.perform(post("/api/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ─── POST /api/payments/verify ────────────────────────────────────────────

    @Test
    void verifyPayment_success_returns200() throws Exception {
        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setRazorpayOrderId("order_abc");
        request.setRazorpayPaymentId("pay_xyz");
        request.setRazorpaySignature("sig_test");

        Map<String, Object> response = Map.of(
                "success", true,
                "message", "Payment received. Awaiting founder approval.",
                "status", "AWAITING_APPROVAL"
        );
        when(paymentService.verifyPayment(any(VerifyPaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─── PUT /api/payments/{id}/accept ────────────────────────────────────────

    @Test
    void acceptPayment_success_returns200() throws Exception {
        Map<String, Object> response = Map.of("success", true, "message", "Investment accepted successfully");
        when(paymentService.acceptPayment(eq(1L))).thenReturn(response);

        mockMvc.perform(put("/api/payments/1/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void acceptPayment_serviceThrows_returns400() throws Exception {
        when(paymentService.acceptPayment(eq(99L)))
                .thenThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(put("/api/payments/99/accept"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payment not found"));
    }

    // ─── PUT /api/payments/{id}/reject ────────────────────────────────────────

    @Test
    void rejectPayment_success_returns200() throws Exception {
        Map<String, Object> response = Map.of("success", true, "message", "Investment rejected");
        when(paymentService.rejectPayment(eq(1L))).thenReturn(response);

        mockMvc.perform(put("/api/payments/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void rejectPayment_serviceThrows_returns400() throws Exception {
        when(paymentService.rejectPayment(eq(99L)))
                .thenThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(put("/api/payments/99/reject"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payment not found"));
    }

    // ─── GET /api/payments/investor/{id} ─────────────────────────────────────

    @Test
    void getPaymentsByInvestor_returnsList() throws Exception {
        Payment p = buildPayment(1L);
        when(paymentService.getPaymentsByInvestor(10L)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/payments/investor/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // ─── GET /api/payments/founder/{id} ──────────────────────────────────────

    @Test
    void getPaymentsByFounder_returnsList() throws Exception {
        Payment p = buildPayment(2L);
        when(paymentService.getPaymentsByFounder(20L)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/payments/founder/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    // ─── GET /api/payments/startup/{id} ──────────────────────────────────────

    @Test
    void getPaymentsByStartup_returnsList() throws Exception {
        Payment p = buildPayment(3L);
        when(paymentService.getPaymentsByStartup(30L)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/payments/startup/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Payment buildPayment(Long id) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setInvestorId(10L);
        payment.setFounderId(20L);
        payment.setStartupId(30L);
        payment.setStartupName("StartupX");
        payment.setInvestorName("InvestorY");
        payment.setAmount(5000.0);
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        return payment;
    }
}