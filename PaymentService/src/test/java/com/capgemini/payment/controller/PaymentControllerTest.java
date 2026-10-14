package com.capgemini.payment.controller;

import com.capgemini.payment.dto.CreateOrderRequest;
import com.capgemini.payment.dto.VerifyPaymentRequest;
import com.capgemini.payment.entity.Payment;
import com.capgemini.payment.saga.PaymentSaga;
import com.capgemini.payment.saga.SagaOrchestrator;
import com.capgemini.payment.saga.SagaStatus;
import com.capgemini.payment.saga.SagaStep;
import com.capgemini.payment.service.IPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.RazorpayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
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
    void createOrder_authenticated_returns200() throws Exception {
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
        when(paymentService.createOrder(any(CreateOrderRequest.class), eq(1L), eq(false))).thenReturn(response);

        mockMvc.perform(post("/api/payments/create-order")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order_123"))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void createOrder_unauthenticated_returns403() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(5000.0);

        mockMvc.perform(post("/api/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void createOrder_serviceThrowsException_returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(5000.0);

        when(paymentService.createOrder(any(CreateOrderRequest.class), eq(1L), eq(false)))
                .thenThrow(new RazorpayException("Razorpay error"));

        mockMvc.perform(post("/api/payments/create-order")
                        .header("X-User-Id", "1")
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
    void acceptPayment_authorizedFounder_returns200() throws Exception {
        Map<String, Object> response = Map.of("success", true, "message", "Investment accepted successfully");
        when(paymentService.acceptPayment(eq(1L), eq(20L), eq(false))).thenReturn(response);

        mockMvc.perform(put("/api/payments/1/accept")
                        .header("X-User-Id", "20")
                        .header("X-User-Roles", "ROLE_FOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void acceptPayment_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/payments/1/accept"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void acceptPayment_unauthorizedCaller_returns403() throws Exception {
        when(paymentService.acceptPayment(eq(1L), eq(999L), eq(false)))
                .thenThrow(new AccessDeniedException("Only the recipient founder or an admin can accept this payment"));

        mockMvc.perform(put("/api/payments/1/accept")
                        .header("X-User-Id", "999")
                        .header("X-User-Roles", "ROLE_FOUNDER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Only the recipient founder or an admin can accept this payment"));
    }

    @Test
    void acceptPayment_invalidStateTransition_returns400() throws Exception {
        when(paymentService.acceptPayment(eq(1L), eq(20L), eq(false)))
                .thenThrow(new IllegalStateException("Payment cannot be accepted in its current status: PENDING"));

        mockMvc.perform(put("/api/payments/1/accept")
                        .header("X-User-Id", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Payment cannot be accepted in its current status: PENDING"));
    }

    @Test
    void acceptPayment_serviceThrows_returns400() throws Exception {
        when(paymentService.acceptPayment(eq(99L), eq(20L), eq(false)))
                .thenThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(put("/api/payments/99/accept")
                        .header("X-User-Id", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payment not found"));
    }

    // ─── PUT /api/payments/{id}/reject ────────────────────────────────────────

    @Test
    void rejectPayment_authorizedFounder_returns200() throws Exception {
        Map<String, Object> response = Map.of("success", true, "message", "Investment rejected");
        when(paymentService.rejectPayment(eq(1L), eq(20L), eq(false))).thenReturn(response);

        mockMvc.perform(put("/api/payments/1/reject")
                        .header("X-User-Id", "20")
                        .header("X-User-Roles", "ROLE_FOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void rejectPayment_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/payments/1/reject"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void rejectPayment_unauthorizedCaller_returns403() throws Exception {
        when(paymentService.rejectPayment(eq(1L), eq(999L), eq(false)))
                .thenThrow(new AccessDeniedException("Only the recipient founder or an admin can reject this payment"));

        mockMvc.perform(put("/api/payments/1/reject")
                        .header("X-User-Id", "999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Only the recipient founder or an admin can reject this payment"));
    }

    @Test
    void rejectPayment_invalidStateTransition_returns400() throws Exception {
        when(paymentService.rejectPayment(eq(1L), eq(20L), eq(false)))
                .thenThrow(new IllegalStateException("Payment cannot be rejected in its current status: SUCCESS"));

        mockMvc.perform(put("/api/payments/1/reject")
                        .header("X-User-Id", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectPayment_serviceThrows_returns400() throws Exception {
        when(paymentService.rejectPayment(eq(99L), eq(20L), eq(false)))
                .thenThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(put("/api/payments/99/reject")
                        .header("X-User-Id", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payment not found"));
    }

    // ─── GET /api/payments/investor/{id} ─────────────────────────────────────

    @Test
    void getPaymentsByInvestor_authorized_returnsList() throws Exception {
        Payment p = buildPayment(1L);
        when(paymentService.getPaymentsByInvestor(10L, 10L, false)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/payments/investor/10")
                        .header("X-User-Id", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getPaymentsByInvestor_crossAccountForbidden_returns403() throws Exception {
        when(paymentService.getPaymentsByInvestor(10L, 999L, false))
                .thenThrow(new AccessDeniedException("Access denied to investor payment history"));

        mockMvc.perform(get("/api/payments/investor/10")
                        .header("X-User-Id", "999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Access denied to investor payment history"));
    }

    @Test
    void getPaymentsByInvestor_adminAllowed_returns200() throws Exception {
        Payment p = buildPayment(1L);
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "999", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(paymentService.getPaymentsByInvestor(10L, 999L, true)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/payments/investor/10")
                        .principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // ─── GET /api/payments/founder/{id} ──────────────────────────────────────

    @Test
    void getPaymentsByFounder_authorized_returnsList() throws Exception {
        Payment p = buildPayment(2L);
        when(paymentService.getPaymentsByFounder(20L, 20L, false)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/payments/founder/20")
                        .header("X-User-Id", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void getPaymentsByFounder_crossAccountForbidden_returns403() throws Exception {
        when(paymentService.getPaymentsByFounder(20L, 999L, false))
                .thenThrow(new AccessDeniedException("Access denied to founder payment history"));

        mockMvc.perform(get("/api/payments/founder/20")
                        .header("X-User-Id", "999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getPaymentsByFounder_adminAllowed_returns200() throws Exception {
        Payment p = buildPayment(2L);
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "999", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(paymentService.getPaymentsByFounder(20L, 999L, true)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/payments/founder/20")
                        .principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    // ─── GET /api/payments/startup/{id} ──────────────────────────────────────

    @Test
    void getPaymentsByStartup_authorized_returnsList() throws Exception {
        Payment p = buildPayment(3L);
        when(paymentService.getPaymentsByStartup(30L, 20L, false)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/payments/startup/30")
                        .header("X-User-Id", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3));
    }

    // ─── GET /api/payments/{id}/saga ──────────────────────────────────────────

    @Test
    void getSagaStatus_participant_returns200() throws Exception {
        PaymentSaga saga = PaymentSaga.builder()
                .id(1L).paymentId(1L)
                .currentStep(SagaStep.ORDER_CREATED)
                .status(SagaStatus.IN_PROGRESS)
                .build();
        when(paymentService.getSagaStatus(1L, 20L, false)).thenReturn(saga);

        mockMvc.perform(get("/api/payments/1/saga")
                        .header("X-User-Id", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1));
    }

    @Test
    void getSagaStatus_unrelatedUser_returns403() throws Exception {
        when(paymentService.getSagaStatus(1L, 999L, false))
                .thenThrow(new AccessDeniedException("Access denied to saga status for payment: 1"));

        mockMvc.perform(get("/api/payments/1/saga")
                        .header("X-User-Id", "999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Access denied to saga status for payment: 1"));
    }

    @Test
    void getSagaStatus_notFound_returns404() throws Exception {
        when(paymentService.getSagaStatus(99L, 20L, false))
                .thenThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(get("/api/payments/99/saga")
                        .header("X-User-Id", "20"))
                .andExpect(status().isNotFound());
    }

    // ─── Admin Role Spoofing Prevention ──────────────────────────────────────

    @Test
    void getPaymentsByInvestor_authenticatedInvestorWithForgedAdminHeader_doesNotGrantAdmin_returns403() throws Exception {
        Authentication investorAuth = new UsernamePasswordAuthenticationToken(
                "10", null, List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));
        when(paymentService.getPaymentsByInvestor(99L, 10L, false))
                .thenThrow(new AccessDeniedException("Access denied to investor payment history"));

        mockMvc.perform(get("/api/payments/investor/99")
                        .principal(investorAuth)
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Access denied to investor payment history"));
    }

    @Test
    void getPaymentsByFounder_authenticatedInvestorWithForgedAdminHeader_doesNotGrantAdmin_returns403() throws Exception {
        Authentication investorAuth = new UsernamePasswordAuthenticationToken(
                "10", null, List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));
        when(paymentService.getPaymentsByFounder(20L, 10L, false))
                .thenThrow(new AccessDeniedException("Access denied to founder payment history"));

        mockMvc.perform(get("/api/payments/founder/20")
                        .principal(investorAuth)
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Access denied to founder payment history"));
    }

    @Test
    void acceptPayment_authenticatedNonAdminWithForgedAdminHeader_doesNotGrantAdmin_returns403() throws Exception {
        Authentication nonAdminAuth = new UsernamePasswordAuthenticationToken(
                "999", null, List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));
        when(paymentService.acceptPayment(1L, 999L, false))
                .thenThrow(new AccessDeniedException("Only the recipient founder or an admin can accept this payment"));

        mockMvc.perform(put("/api/payments/1/accept")
                        .principal(nonAdminAuth)
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Only the recipient founder or an admin can accept this payment"));
    }

    @Test
    void rejectPayment_authenticatedNonAdminWithForgedAdminHeader_doesNotGrantAdmin_returns403() throws Exception {
        Authentication nonAdminAuth = new UsernamePasswordAuthenticationToken(
                "999", null, List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));
        when(paymentService.rejectPayment(1L, 999L, false))
                .thenThrow(new AccessDeniedException("Only the recipient founder or an admin can reject this payment"));

        mockMvc.perform(put("/api/payments/1/reject")
                        .principal(nonAdminAuth)
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Only the recipient founder or an admin can reject this payment"));
    }

    @Test
    void acceptPayment_legitimateAdmin_returns200() throws Exception {
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "999", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(paymentService.acceptPayment(1L, 999L, true))
                .thenReturn(Map.of("success", true, "message", "Investment accepted successfully"));

        mockMvc.perform(put("/api/payments/1/accept")
                        .principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void rejectPayment_legitimateAdmin_returns200() throws Exception {
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "999", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(paymentService.rejectPayment(1L, 999L, true))
                .thenReturn(Map.of("success", true, "message", "Investment rejected"));

        mockMvc.perform(put("/api/payments/1/reject")
                        .principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getPaymentsByFounder_unauthenticatedWithForgedAdminHeader_doesNotGrantAdmin_returns403() throws Exception {
        when(paymentService.getPaymentsByFounder(20L, 999L, false))
                .thenThrow(new AccessDeniedException("Access denied to founder payment history"));

        mockMvc.perform(get("/api/payments/founder/20")
                        .header("X-User-Id", "999")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getPaymentsByFounder_anonymousUserWithForgedAdminHeader_doesNotGrantAdmin_returns403() throws Exception {
        Authentication anonAuth = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        when(paymentService.getPaymentsByFounder(20L, 999L, false))
                .thenThrow(new AccessDeniedException("Access denied to founder payment history"));

        mockMvc.perform(get("/api/payments/founder/20")
                        .principal(anonAuth)
                        .header("X-User-Id", "999")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
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