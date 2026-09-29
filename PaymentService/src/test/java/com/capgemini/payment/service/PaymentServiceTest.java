package com.capgemini.payment.service;

import com.capgemini.payment.dto.CreateOrderRequest;
import com.capgemini.payment.dto.VerifyPaymentRequest;
import com.capgemini.payment.entity.Payment;
import com.capgemini.payment.repository.PaymentRepository;
import com.capgemini.payment.saga.PaymentSaga;
import com.capgemini.payment.saga.SagaOrchestrator;
import com.capgemini.payment.saga.SagaStatus;
import com.capgemini.payment.saga.SagaStep;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private SagaOrchestrator sagaOrchestrator;

    @Mock
    private OrderClient ordersMock;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "test_key_id");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_key_secret");
        ReflectionTestUtils.setField(razorpayClient, "orders", ordersMock);

        // Default saga stubs
        when(sagaOrchestrator.startSaga(any(Payment.class))).thenReturn(buildSaga());
        doNothing().when(sagaOrchestrator).onPaymentCaptured(any(Payment.class));
        doNothing().when(sagaOrchestrator).onAwaitingApproval(any(Payment.class));
        doNothing().when(sagaOrchestrator).onPaymentAccepted(any(Payment.class));
        when(sagaOrchestrator.compensate(any(Payment.class))).thenReturn(true);
        doNothing().when(sagaOrchestrator).onStepFailed(anyLong(), anyString());
    }

    // ─── createOrder ──────────────────────────────────────────────────────────

    @Test
    void createOrder_success_startsSaga() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setInvestorId(1L);
        request.setFounderId(2L);
        request.setStartupId(3L);
        request.setStartupName("StartupX");
        request.setInvestorName("InvestorY");
        request.setAmount(5000.0);

        Order razorpayOrder = mock(Order.class);
        when(razorpayOrder.get("id")).thenReturn("order_123");
        when(ordersMock.create(any())).thenReturn(razorpayOrder);

        Payment savedPayment = buildPayment();
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        Map<String, Object> result = paymentService.createOrder(request);

        assertThat(result.get("orderId")).isEqualTo("order_123");
        assertThat(result.get("currency")).isEqualTo("INR");
        assertThat(result.get("keyId")).isEqualTo("test_key_id");
        verify(paymentRepository).save(any(Payment.class));
        verify(sagaOrchestrator).startSaga(savedPayment);
    }

    @Test
    void createOrder_razorpayException_propagates() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(1000.0);
        when(ordersMock.create(any())).thenThrow(new RazorpayException("Razorpay error"));

        org.junit.jupiter.api.Assertions.assertThrows(RazorpayException.class,
                () -> paymentService.createOrder(request));
        verify(sagaOrchestrator, never()).startSaga(any());
    }

    // ─── verifyPayment ────────────────────────────────────────────────────────

    @Test
    void verifyPayment_validSignature_advancesSagaToAwaitingApproval() {
        VerifyPaymentRequest request = buildVerifyRequest();
        Payment payment = buildPayment();
        when(paymentRepository.findByRazorpayOrderId("order_abc")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(), eq("test_key_secret"))).thenReturn(true);

            Map<String, Object> result = paymentService.verifyPayment(request);

            assertThat(result.get("success")).isEqualTo(true);
            assertThat(result.get("status")).isEqualTo("AWAITING_APPROVAL");
            verify(sagaOrchestrator).onPaymentCaptured(any(Payment.class));
            verify(sagaOrchestrator).onAwaitingApproval(any(Payment.class));
        }
    }

    @Test
    void verifyPayment_invalidSignature_recordsSagaFailure() {
        VerifyPaymentRequest request = buildVerifyRequest();
        Payment payment = buildPayment();
        when(paymentRepository.findByRazorpayOrderId("order_abc")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(), eq("test_key_secret"))).thenReturn(false);

            Map<String, Object> result = paymentService.verifyPayment(request);

            assertThat(result.get("success")).isEqualTo(false);
            verify(sagaOrchestrator).onStepFailed(eq(payment.getId()), anyString());
        }
    }

    @Test
    void verifyPayment_signatureThrows_returnsFalse() {
        VerifyPaymentRequest request = buildVerifyRequest();

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(), anyString()))
                    .thenThrow(new RuntimeException("Signature error"));

            Map<String, Object> result = paymentService.verifyPayment(request);

            assertThat(result.get("success")).isEqualTo(false);
            assertThat(result.get("message").toString()).contains("Signature verification error");
        }
    }

    @Test
    void verifyPayment_paymentNotFound_returnsFalse() {
        VerifyPaymentRequest request = buildVerifyRequest();
        when(paymentRepository.findByRazorpayOrderId("order_abc")).thenReturn(Optional.empty());

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(), anyString())).thenReturn(true);

            Map<String, Object> result = paymentService.verifyPayment(request);

            assertThat(result.get("success")).isEqualTo(false);
            assertThat(result.get("message").toString()).contains("Payment record not found");
        }
    }

    // ─── acceptPayment ────────────────────────────────────────────────────────

    @Test
    void acceptPayment_success_completesSaga() {
        Payment payment = buildPayment();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Map<String, Object> result = paymentService.acceptPayment(1L);

        assertThat(result.get("success")).isEqualTo(true);
        verify(paymentRepository).save(argThat(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS));
        verify(sagaOrchestrator).onPaymentAccepted(any(Payment.class));
    }

    @Test
    void acceptPayment_paymentNotFound_throwsException() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> paymentService.acceptPayment(99L));
        verify(sagaOrchestrator, never()).onPaymentAccepted(any());
    }

    // ─── rejectPayment ────────────────────────────────────────────────────────

    @Test
    void rejectPayment_compensationSuccess_returnsSuccessMessage() {
        Payment payment = buildPayment();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(sagaOrchestrator.compensate(any(Payment.class))).thenReturn(true);

        Map<String, Object> result = paymentService.rejectPayment(1L);

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("message").toString()).contains("refund initiated");
        verify(paymentRepository).save(argThat(p -> p.getStatus() == Payment.PaymentStatus.REJECTED));
        verify(sagaOrchestrator).compensate(any(Payment.class));
    }

    @Test
    void rejectPayment_compensationFails_returnsErrorMessage() {
        Payment payment = buildPayment();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(sagaOrchestrator.compensate(any(Payment.class))).thenReturn(false);

        Map<String, Object> result = paymentService.rejectPayment(1L);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message").toString()).contains("contact support");
    }

    @Test
    void rejectPayment_paymentNotFound_throwsException() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> paymentService.rejectPayment(99L));
        verify(sagaOrchestrator, never()).compensate(any());
    }

    // ─── query methods ────────────────────────────────────────────────────────

    @Test
    void getPaymentsByInvestor_returnsList() {
        when(paymentRepository.findByInvestorIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(buildPayment(), buildPayment()));

        assertThat(paymentService.getPaymentsByInvestor(1L)).hasSize(2);
    }

    @Test
    void getPaymentsByFounder_returnsList() {
        when(paymentRepository.findByFounderIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(buildPayment()));

        assertThat(paymentService.getPaymentsByFounder(2L)).hasSize(1);
    }

    @Test
    void getPaymentsByStartup_emptyList() {
        when(paymentRepository.findByStartupIdOrderByCreatedAtDesc(3L)).thenReturn(List.of());

        assertThat(paymentService.getPaymentsByStartup(3L)).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private VerifyPaymentRequest buildVerifyRequest() {
        VerifyPaymentRequest r = new VerifyPaymentRequest();
        r.setRazorpayOrderId("order_abc");
        r.setRazorpayPaymentId("pay_xyz");
        r.setRazorpaySignature("sig_test");
        return r;
    }

    private Payment buildPayment() {
        Payment p = new Payment();
        p.setId(1L);
        p.setRazorpayOrderId("order_abc");
        p.setRazorpayPaymentId("pay_xyz");
        p.setInvestorId(10L);
        p.setFounderId(20L);
        p.setStartupId(30L);
        p.setStartupName("StartupX");
        p.setInvestorName("InvestorY");
        p.setAmount(5000.0);
        p.setStatus(Payment.PaymentStatus.PENDING);
        return p;
    }

    private PaymentSaga buildSaga() {
        return PaymentSaga.builder()
                .id(1L).paymentId(1L)
                .currentStep(SagaStep.ORDER_CREATED)
                .status(SagaStatus.IN_PROGRESS)
                .build();
    }
}
