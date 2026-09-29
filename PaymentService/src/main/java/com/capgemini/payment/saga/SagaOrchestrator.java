package com.capgemini.payment.saga;

import com.capgemini.payment.config.RabbitMQConfig;
import com.capgemini.payment.dto.PaymentEventDTO;
import com.capgemini.payment.entity.Payment;
import com.capgemini.payment.service.EmailService;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Choreography-based Saga Orchestrator for the payment flow.
 *
 * Each step is recorded in payment_sagas. If the founder rejects (or any
 * critical step fails), compensating transactions are executed in reverse order:
 *
 *   Forward:   ORDER_CREATED → PAYMENT_CAPTURED → AWAITING_APPROVAL → ACCEPTED
 *   Compensate:                COMPENSATING (refund) → COMPENSATED
 *
 * If compensation itself fails the saga is marked FAILED and requires manual
 * intervention (the failure reason is persisted for auditing).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final PaymentSagaRepository sagaRepository;
    private final RabbitTemplate rabbitTemplate;
    private final EmailService emailService;
    private final RazorpayClient razorpayClient;

    // ── Step 1: Order created ─────────────────────────────────────────────────

    @Transactional
    public PaymentSaga startSaga(Payment payment) {
        PaymentSaga saga = PaymentSaga.builder()
                .paymentId(payment.getId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .currentStep(SagaStep.ORDER_CREATED)
                .status(SagaStatus.IN_PROGRESS)
                .build();
        PaymentSaga saved = sagaRepository.save(saga);
        log.info("[SAGA] Started. paymentId={}, sagaId={}", payment.getId(), saved.getId());
        return saved;
    }

    // ── Step 2: Payment captured by Razorpay ─────────────────────────────────

    @Transactional
    public void onPaymentCaptured(Payment payment) {
        PaymentSaga saga = findSaga(payment.getId());
        saga.setCurrentStep(SagaStep.PAYMENT_CAPTURED);
        saga.setRazorpayPaymentId(payment.getRazorpayPaymentId());
        sagaRepository.save(saga);
        log.info("[SAGA] Step PAYMENT_CAPTURED. paymentId={}", payment.getId());
    }

    // ── Step 3: Awaiting founder approval ─────────────────────────────────────

    @Transactional
    public void onAwaitingApproval(Payment payment) {
        PaymentSaga saga = findSaga(payment.getId());
        saga.setCurrentStep(SagaStep.AWAITING_APPROVAL);
        sagaRepository.save(saga);
        log.info("[SAGA] Step AWAITING_APPROVAL. paymentId={}", payment.getId());

        // Publish event so NotificationService alerts the founder
        publishEvent(payment, RabbitMQConfig.PAYMENT_PENDING_KEY, "AWAITING_APPROVAL");
    }

    // ── Step 4a: Founder accepted — saga complete ─────────────────────────────

    @Transactional
    public void onPaymentAccepted(Payment payment) {
        PaymentSaga saga = findSaga(payment.getId());
        saga.setCurrentStep(SagaStep.ACCEPTED);
        saga.setStatus(SagaStatus.COMPLETED);
        sagaRepository.save(saga);
        log.info("[SAGA] COMPLETED (accepted). paymentId={}", payment.getId());

        // Publish success event → NotificationService notifies investor
        publishEvent(payment, RabbitMQConfig.PAYMENT_SUCCESS_KEY, "SUCCESS");

        // Send confirmation emails
        sendEmailSafely(() -> emailService.sendPaymentSuccessEmailToInvestor(payment),
                "success email to investor", payment.getId());
        sendEmailSafely(() -> emailService.sendPaymentReceivedEmailToFounder(payment),
                "success email to founder", payment.getId());
    }

    // ── Step 4b: Founder rejected — run compensating transactions ─────────────

    @Transactional
    public boolean compensate(Payment payment) {
        // Saga record may not exist for legacy payments — don't let that block notifications
        PaymentSaga saga = sagaRepository.findByPaymentId(payment.getId()).orElse(null);
        if (saga != null) {
            saga.setCurrentStep(SagaStep.COMPENSATING);
            saga.setStatus(SagaStatus.COMPENSATING);
            sagaRepository.save(saga);
        }
        log.info("[SAGA] Compensation started. paymentId={}", payment.getId());

        // Compensating transaction 1: Refund investor via Razorpay
        boolean refundSucceeded = false;
        if (payment.getRazorpayPaymentId() != null) {
            try {
                int amountInPaise = (int) (payment.getAmount() * 100);
                JSONObject refundOptions = new JSONObject();
                refundOptions.put("amount", amountInPaise);
                refundOptions.put("speed", "normal");
                razorpayClient.payments.refund(payment.getRazorpayPaymentId(), refundOptions);
                refundSucceeded = true;
                log.info("[SAGA] Razorpay refund issued. razorpayPaymentId={}", payment.getRazorpayPaymentId());
            } catch (Exception e) {
                if (saga != null) saga.setFailureReason("Razorpay refund failed: " + e.getMessage());
                log.error("[SAGA] Razorpay refund failed (non-blocking). paymentId={}, reason={}", payment.getId(), e.getMessage());
            }
        }

        // Compensating transaction 2: Always publish rejection event so both parties are notified
        publishEvent(payment, RabbitMQConfig.PAYMENT_FAILED_KEY, "REJECTED");

        // Compensating transaction 3: Send rejection email to investor
        sendEmailSafely(() -> emailService.sendPaymentRejectedEmailToInvestor(payment),
                "rejection email to investor", payment.getId());

        // Update saga final state
        if (saga != null) {
            if (refundSucceeded || payment.getRazorpayPaymentId() == null) {
                saga.setCurrentStep(SagaStep.COMPENSATED);
                saga.setStatus(SagaStatus.COMPENSATED);
            } else {
                saga.setCurrentStep(SagaStep.FAILED);
                saga.setStatus(SagaStatus.FAILED);
            }
            sagaRepository.save(saga);
        }

        boolean success = refundSucceeded || payment.getRazorpayPaymentId() == null;
        log.info("[SAGA] Compensation {}. paymentId={}", success ? "COMPLETE" : "partial (refund failed)", payment.getId());
        return success;
    }

    // ── Failure hook — called when a non-compensatable step fails ─────────────

    @Transactional
    public void onStepFailed(Long paymentId, String reason) {
        sagaRepository.findByPaymentId(paymentId).ifPresent(saga -> {
            saga.setStatus(SagaStatus.FAILED);
            saga.setFailureReason(reason);
            sagaRepository.save(saga);
            log.error("[SAGA] Step failed. paymentId={}, reason={}", paymentId, reason);
        });
    }

    // ── Read-only saga status ─────────────────────────────────────────────────

    public PaymentSaga getSagaByPaymentId(Long paymentId) {
        return findSaga(paymentId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PaymentSaga findSaga(Long paymentId) {
        return sagaRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Saga not found for paymentId: " + paymentId));
    }

    private void publishEvent(Payment payment, String routingKey, String status) {
        try {
            PaymentEventDTO event = new PaymentEventDTO(
                    payment.getId(), payment.getInvestorId(), payment.getFounderId(),
                    payment.getStartupId(), payment.getStartupName(), payment.getInvestorName(),
                    payment.getAmount(), payment.getRazorpayPaymentId(), status
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, routingKey, event);
            log.info("[SAGA] Event published. routingKey={}, paymentId={}", routingKey, payment.getId());
        } catch (Exception e) {
            log.warn("[SAGA] RabbitMQ publish failed (non-critical). routingKey={}, error={}", routingKey, e.getMessage());
        }
    }

    private void sendEmailSafely(Runnable emailAction, String description, Long paymentId) {
        try {
            emailAction.run();
        } catch (Exception e) {
            log.warn("[SAGA] {} failed (non-critical). paymentId={}, error={}", description, paymentId, e.getMessage());
        }
    }
}
