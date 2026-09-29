package com.capgemini.payment.saga;

import com.capgemini.payment.config.RabbitMQConfig;
import com.capgemini.payment.entity.Payment;
import com.capgemini.payment.service.EmailService;
import com.razorpay.PaymentClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SagaOrchestratorTest {

    @Mock
    private PaymentSagaRepository sagaRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private EmailService emailService;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private PaymentClient paymentsMock;

    @InjectMocks
    private SagaOrchestrator sagaOrchestrator;

    private Payment payment;
    private PaymentSaga saga;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(razorpayClient, "payments", paymentsMock);

        payment = new Payment();
        payment.setId(1L);
        payment.setRazorpayOrderId("order_abc");
        payment.setRazorpayPaymentId("pay_xyz");
        payment.setInvestorId(10L);
        payment.setFounderId(20L);
        payment.setStartupId(30L);
        payment.setStartupName("StartupX");
        payment.setInvestorName("InvestorY");
        payment.setAmount(5000.0);

        saga = PaymentSaga.builder()
                .id(1L)
                .paymentId(1L)
                .razorpayOrderId("order_abc")
                .currentStep(SagaStep.ORDER_CREATED)
                .status(SagaStatus.IN_PROGRESS)
                .build();

        when(sagaRepository.save(any(PaymentSaga.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sagaRepository.findByPaymentId(1L)).thenReturn(Optional.of(saga));
    }

    // ─── startSaga ────────────────────────────────────────────────────────────

    @Test
    void startSaga_createsSagaInProgressAtOrderCreated() {
        PaymentSaga result = sagaOrchestrator.startSaga(payment);

        assertThat(result.getPaymentId()).isEqualTo(1L);
        assertThat(result.getCurrentStep()).isEqualTo(SagaStep.ORDER_CREATED);
        assertThat(result.getStatus()).isEqualTo(SagaStatus.IN_PROGRESS);
        verify(sagaRepository).save(any(PaymentSaga.class));
    }

    // ─── onPaymentCaptured ────────────────────────────────────────────────────

    @Test
    void onPaymentCaptured_updatesStepToPaymentCaptured() {
        sagaOrchestrator.onPaymentCaptured(payment);

        verify(sagaRepository).save(argThat(s ->
                s.getCurrentStep() == SagaStep.PAYMENT_CAPTURED
                && s.getRazorpayPaymentId().equals("pay_xyz")));
    }

    // ─── onAwaitingApproval ───────────────────────────────────────────────────

    @Test
    void onAwaitingApproval_updatesStepAndPublishesEvent() {
        sagaOrchestrator.onAwaitingApproval(payment);

        verify(sagaRepository).save(argThat(s -> s.getCurrentStep() == SagaStep.AWAITING_APPROVAL));
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.PAYMENT_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_PENDING_KEY),
                any(Object.class));
    }

    @Test
    void onAwaitingApproval_rabbitMQFails_sagaStillUpdated() {
        doThrow(new RuntimeException("RabbitMQ down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        sagaOrchestrator.onAwaitingApproval(payment);

        verify(sagaRepository).save(argThat(s -> s.getCurrentStep() == SagaStep.AWAITING_APPROVAL));
    }

    // ─── onPaymentAccepted ────────────────────────────────────────────────────

    @Test
    void onPaymentAccepted_completesAndPublishesSuccessEvent() {
        sagaOrchestrator.onPaymentAccepted(payment);

        verify(sagaRepository).save(argThat(s ->
                s.getCurrentStep() == SagaStep.ACCEPTED
                && s.getStatus() == SagaStatus.COMPLETED));
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.PAYMENT_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_SUCCESS_KEY),
                any(Object.class));
        verify(emailService).sendPaymentSuccessEmailToInvestor(payment);
        verify(emailService).sendPaymentReceivedEmailToFounder(payment);
    }

    @Test
    void onPaymentAccepted_emailFails_sagaStillCompleted() {
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendPaymentSuccessEmailToInvestor(any());

        sagaOrchestrator.onPaymentAccepted(payment);

        verify(sagaRepository).save(argThat(s -> s.getStatus() == SagaStatus.COMPLETED));
    }

    // ─── compensate ───────────────────────────────────────────────────────────

    @Test
    void compensate_success_issuesRefundAndPublishesRejectionEvent() throws Exception {
        when(paymentsMock.refund(eq("pay_xyz"), any(JSONObject.class))).thenReturn(null);

        boolean result = sagaOrchestrator.compensate(payment);

        assertThat(result).isTrue();
        verify(paymentsMock).refund(eq("pay_xyz"), any(JSONObject.class));
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.PAYMENT_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_FAILED_KEY),
                any(Object.class));
        verify(emailService).sendPaymentRejectedEmailToInvestor(payment);
        // Saga should be COMPENSATED
        verify(sagaRepository, atLeast(2)).save(argThat(s ->
                s.getStatus() == SagaStatus.COMPENSATED
                || s.getStatus() == SagaStatus.COMPENSATING));
    }

    @Test
    void compensate_razorpayRefundFails_sagaMarkedFailed() throws Exception {
        when(paymentsMock.refund(anyString(), any(JSONObject.class)))
                .thenThrow(new RazorpayException("Refund failed"));

        boolean result = sagaOrchestrator.compensate(payment);

        assertThat(result).isFalse();
        // Saga is marked FAILED with failure reason
        verify(sagaRepository, atLeast(1)).save(argThat(s ->
                s.getStatus() == SagaStatus.FAILED
                && s.getFailureReason() != null));
        // Rejection event is ALWAYS published so both parties are notified even if refund fails
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.PAYMENT_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_FAILED_KEY),
                any(Object.class));
    }

    @Test
    void compensate_noRazorpayPaymentId_skipsRefundAndCompensates() throws Exception {
        payment.setRazorpayPaymentId(null);

        boolean result = sagaOrchestrator.compensate(payment);

        assertThat(result).isTrue();
        verify(paymentsMock, never()).refund(anyString(), any(JSONObject.class));
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // ─── onStepFailed ─────────────────────────────────────────────────────────

    @Test
    void onStepFailed_marksSagaAsFailedWithReason() {
        sagaOrchestrator.onStepFailed(1L, "Signature invalid");

        verify(sagaRepository).save(argThat(s ->
                s.getStatus() == SagaStatus.FAILED
                && s.getFailureReason().equals("Signature invalid")));
    }

    @Test
    void onStepFailed_sagaNotFound_doesNothing() {
        when(sagaRepository.findByPaymentId(999L)).thenReturn(Optional.empty());

        sagaOrchestrator.onStepFailed(999L, "reason");

        verify(sagaRepository, never()).save(any());
    }

    // ─── getSagaByPaymentId ───────────────────────────────────────────────────

    @Test
    void getSagaByPaymentId_found_returnsSaga() {
        PaymentSaga result = sagaOrchestrator.getSagaByPaymentId(1L);

        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo(1L);
    }

    @Test
    void getSagaByPaymentId_notFound_throwsException() {
        when(sagaRepository.findByPaymentId(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> sagaOrchestrator.getSagaByPaymentId(999L));
    }
}
