package com.capgemini.notification.listener;

import com.capgemini.notification.enums.NotificationType;
import com.capgemini.notification.event.InvestmentApprovedEvent;
import com.capgemini.notification.event.InvestmentCreatedEvent;
import com.capgemini.notification.event.StartupCreatedEvent;
import com.capgemini.notification.event.StartupRejectedEvent;
import com.capgemini.notification.event.TeamInviteSentEvent;
import com.capgemini.notification.service.EmailService;
import com.capgemini.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final EmailService emailService;

    @RabbitListener(queues = "${rabbitmq.queue.user-registered}")
    public void handleUserRegistered(Map<String, Object> payload) {
        try {
            Long userId   = payload.get("userId") != null ? ((Number) payload.get("userId")).longValue() : null;
            String name   = (String) payload.get("name");
            String email  = (String) payload.get("email");
            String role   = (String) payload.get("role");
            log.info("Received user.registered event — userId={} email={} role={}", userId, email, role);

            if (userId != null) {
                notificationService.createNotification(
                        userId,
                        "Welcome to FounderLink, " + name + "! Your " + formatRole(role) + " profile has been created successfully.",
                        NotificationType.USER_REGISTERED
                );
            }
        } catch (Exception e) {
            log.error("Failed to process user.registered event: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.startup-created}")
    public void handleStartupCreated(StartupCreatedEvent event) {
        log.info("Received startup created event for startupId: {}", event.getStartupId());
        notificationService.createNotification(
                event.getFounderId(),
                "Your startup has been submitted for review. Startup ID: " + event.getStartupId(),
                NotificationType.STARTUP_CREATED
        );
    }

    @RabbitListener(queues = "${rabbitmq.queue.investment-created}")
    public void handleInvestmentCreated(InvestmentCreatedEvent event) {
        log.info("Received investment created event for startupId: {}", event.getStartupId());
        notificationService.createNotification(
                event.getFounderId(),
                "New investment request of amount " + event.getAmount() + " received for your startup.",
                NotificationType.INVESTMENT_CREATED
        );
    }

    @RabbitListener(queues = "${rabbitmq.queue.investment-approved}")
    public void handleInvestmentApproved(InvestmentApprovedEvent event) {
        log.info("Received investment approved event for investorId: {}", event.getInvestorId());
        notificationService.createNotification(
                event.getInvestorId(),
                "Your investment of amount " + event.getAmount() + " has been approved.",
                NotificationType.INVESTMENT_APPROVED
        );
    }

    @RabbitListener(queues = "${rabbitmq.queue.startup-rejected}")
    public void handleStartupRejected(StartupRejectedEvent event) {
        log.info("Received startup rejected event for startupId: {}", event.getStartupId());
        notificationService.createNotification(
                event.getFounderId(),
                "We're sorry, your startup \"" + event.getStartupName() + "\" has been reviewed and was not approved at this time. We're not moving forward with this submission.",
                NotificationType.STARTUP_REJECTED
        );
    }

    @RabbitListener(queues = "${rabbitmq.queue.team-invite-sent}")
    public void handleTeamInvite(TeamInviteSentEvent event) {
        log.info("Received team invite event for userId: {}", event.getInvitedUserId());
        notificationService.createNotification(
                event.getInvitedUserId(),
                "You have been invited to join a startup team as " + event.getRole(),
                NotificationType.TEAM_INVITE_SENT
        );
    }

    // Use Map<String, Object> to avoid TypeId deserialization mismatch between PaymentService and NotificationService
    @RabbitListener(queues = "${rabbitmq.queue.payment-failed}")
    public void handlePaymentFailed(Map<String, Object> payload) {
        try {
            Long investorId = payload.get("investorId") != null ? ((Number) payload.get("investorId")).longValue() : null;
            Long founderId  = payload.get("founderId")  != null ? ((Number) payload.get("founderId")).longValue()  : null;
            String startupName  = (String) payload.get("startupName");
            String investorName = (String) payload.get("investorName");
            Number amount       = payload.get("amount") != null ? (Number) payload.get("amount") : 0;
            log.info("Received payment rejected event — investorId={} founderId={} startup={}", investorId, founderId, startupName);

            if (investorId != null) {
                notificationService.createNotification(
                        investorId,
                        "Your investment of ₹" + amount.longValue() + " in " + startupName + " was rejected by the founder. A refund has been initiated to your account.",
                        NotificationType.PAYMENT_REJECTED
                );
            }
            if (founderId != null) {
                notificationService.createNotification(
                        founderId,
                        "You rejected the investment of ₹" + amount.longValue() + " from " + investorName + " for " + startupName + ". A refund has been issued to the investor.",
                        NotificationType.PAYMENT_REJECTED
                );
            }
        } catch (Exception e) {
            log.error("Failed to process payment rejected event: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.payment-success}")
    public void handlePaymentSuccess(Map<String, Object> payload) {
        try {
            Long investorId = payload.get("investorId") != null ? ((Number) payload.get("investorId")).longValue() : null;
            Long founderId  = payload.get("founderId")  != null ? ((Number) payload.get("founderId")).longValue()  : null;
            String startupName  = (String) payload.get("startupName");
            String investorName = (String) payload.get("investorName");
            Number amount       = payload.get("amount") != null ? (Number) payload.get("amount") : 0;
            log.info("Received payment success event — investorId={} founderId={} startup={}", investorId, founderId, startupName);

            if (investorId != null) {
                notificationService.createNotification(
                        investorId,
                        "Your payment of ₹" + amount.longValue() + " for " + startupName + " was accepted by the founder!",
                        NotificationType.PAYMENT_SUCCESS
                );
            }
            if (founderId != null) {
                notificationService.createNotification(
                        founderId,
                        investorName + " has invested ₹" + amount.longValue() + " in " + startupName + ". Investment confirmed.",
                        NotificationType.PAYMENT_SUCCESS
                );
            }
        } catch (Exception e) {
            log.error("Failed to process payment success event: {}", e.getMessage());
        }
    }

    private String formatRole(String role) {
        return switch (role) {
            case "ROLE_FOUNDER"   -> "Founder";
            case "ROLE_INVESTOR"  -> "Investor";
            case "ROLE_COFOUNDER" -> "Co-Founder";
            default               -> "member";
        };
    }
}
