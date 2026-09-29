package com.capgemini.payment.service;

import com.capgemini.payment.entity.Payment;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@founderlink.com");
    }

    // ─── sendPaymentSuccessEmailToInvestor ───────────────────────────────────

    @Test
    void sendPaymentSuccessEmailToInvestor_success_sendsEmail() {
        Payment payment = buildPayment();
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPaymentSuccessEmailToInvestor(payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPaymentSuccessEmailToInvestor_mailSenderThrows_doesNotPropagate() {
        Payment payment = buildPayment();
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP failure")).when(mailSender).send(any(MimeMessage.class));

        // Should NOT throw — exception is caught internally
        emailService.sendPaymentSuccessEmailToInvestor(payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    // ─── sendPaymentReceivedEmailToFounder ───────────────────────────────────

    @Test
    void sendPaymentReceivedEmailToFounder_success_sendsEmail() {
        Payment payment = buildPayment();
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPaymentReceivedEmailToFounder(payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPaymentReceivedEmailToFounder_mailSenderThrows_doesNotPropagate() {
        Payment payment = buildPayment();
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP failure")).when(mailSender).send(any(MimeMessage.class));

        // Should NOT throw — exception is caught internally
        emailService.sendPaymentReceivedEmailToFounder(payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Payment buildPayment() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvestorId(10L);
        payment.setFounderId(20L);
        payment.setStartupId(30L);
        payment.setStartupName("StartupX");
        payment.setInvestorName("InvestorY");
        payment.setInvestorEmail("investor@example.com");
        payment.setFounderEmail("founder@example.com");
        payment.setAmount(5000.0);
        payment.setRazorpayPaymentId("pay_xyz");
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        return payment;
    }
}
