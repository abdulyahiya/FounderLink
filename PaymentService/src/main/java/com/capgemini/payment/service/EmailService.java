package com.capgemini.payment.service;

import com.capgemini.payment.entity.Payment;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPaymentSuccessEmailToInvestor(Payment payment) {
        String to = payment.getInvestorEmail();
        if (to == null || to.isBlank()) {
            log.warn("Investor email not available for paymentId={}, skipping email", payment.getId());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Investment Confirmed — FounderLink 🎉");
            helper.setText(buildInvestorSuccessHtml(payment), true);
            mailSender.send(message);
            log.info("Payment success email sent to investor: {}", to);
        } catch (Exception e) {
            log.error("Failed to send investor success email: {}", e.getMessage());
        }
    }

    public void sendPaymentRejectedEmailToInvestor(Payment payment) {
        String to = payment.getInvestorEmail();
        if (to == null || to.isBlank()) {
            log.warn("Investor email not available for paymentId={}, skipping email", payment.getId());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Investment Update — FounderLink");
            helper.setText(buildInvestorRejectedHtml(payment), true);
            mailSender.send(message);
            log.info("Payment rejection email sent to investor: {}", to);
        } catch (Exception e) {
            log.error("Failed to send investor rejection email: {}", e.getMessage());
        }
    }

    public void sendPaymentReceivedEmailToFounder(Payment payment) {
        String to = payment.getFounderEmail();
        if (to == null || to.isBlank()) {
            log.warn("Founder email not available for paymentId={}, skipping email", payment.getId());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("New Investment Confirmed — FounderLink 💰");
            helper.setText(buildFounderSuccessHtml(payment), true);
            mailSender.send(message);
            log.info("Payment received email sent to founder: {}", to);
        } catch (Exception e) {
            log.error("Failed to send founder email: {}", e.getMessage());
        }
    }

    private String buildInvestorSuccessHtml(Payment payment) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background-color:#0f1117;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#0f1117;padding:40px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" style="background-color:#1a1d27;border-radius:16px;overflow:hidden;border:1px solid #2d3148;">
                        <tr>
                          <td style="background:linear-gradient(135deg,#059669,#10b981);padding:36px 40px;text-align:center;">
                            <h1 style="margin:0;color:#ffffff;font-size:26px;font-weight:700;">FounderLink</h1>
                            <p style="margin:6px 0 0;color:#a7f3d0;font-size:14px;">Investment Confirmed 🎉</p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:36px 40px;">
                            <h2 style="margin:0 0 6px;color:#ffffff;font-size:20px;">Hi %s,</h2>
                            <p style="margin:0 0 24px;color:#9ca3af;font-size:15px;">Your investment has been accepted by the founder. Here are the details:</p>
                            <div style="background:#12152a;border:1px solid #1e3a5f;border-radius:12px;padding:20px 24px;margin-bottom:28px;">
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr><td style="color:#6b7280;font-size:13px;padding:6px 0;">Startup</td><td style="color:#ffffff;font-size:13px;font-weight:600;text-align:right;">%s</td></tr>
                                <tr><td style="color:#6b7280;font-size:13px;padding:6px 0;">Amount</td><td style="color:#10b981;font-size:15px;font-weight:700;text-align:right;">₹%s</td></tr>
                                <tr><td style="color:#6b7280;font-size:13px;padding:6px 0;">Payment ID</td><td style="color:#9ca3af;font-size:12px;font-family:monospace;text-align:right;">%s</td></tr>
                                <tr><td style="color:#6b7280;font-size:13px;padding:6px 0;">Status</td><td style="text-align:right;"><span style="background:#065f46;color:#6ee7b7;font-size:12px;font-weight:600;padding:3px 10px;border-radius:20px;">CONFIRMED</span></td></tr>
                              </table>
                            </div>
                            <p style="color:#9ca3af;font-size:14px;">Thank you for investing through FounderLink. You can track your portfolio in the app.</p>
                            <div style="text-align:center;margin-top:28px;">
                              <a href="http://localhost:3000/investor/payments" style="display:inline-block;background:linear-gradient(135deg,#059669,#10b981);color:#fff;text-decoration:none;padding:13px 32px;border-radius:10px;font-size:14px;font-weight:600;">View My Investments →</a>
                            </div>
                          </td>
                        </tr>
                        <tr><td style="padding:20px 40px;border-top:1px solid #2d3148;text-align:center;">
                          <p style="margin:0;color:#4b5563;font-size:12px;">FounderLink · Connect Build Grow Together</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                payment.getInvestorName(),
                payment.getStartupName(),
                String.format("%,.0f", payment.getAmount()),
                payment.getRazorpayPaymentId() != null ? payment.getRazorpayPaymentId() : "N/A"
        );
    }

    private String buildInvestorRejectedHtml(Payment payment) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background-color:#0f1117;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#0f1117;padding:40px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" style="background-color:#1a1d27;border-radius:16px;overflow:hidden;border:1px solid #2d3148;">
                        <tr>
                          <td style="background:linear-gradient(135deg,#1f2937,#374151);padding:36px 40px;text-align:center;">
                            <h1 style="margin:0;color:#ffffff;font-size:26px;font-weight:700;">FounderLink</h1>
                            <p style="margin:6px 0 0;color:#9ca3af;font-size:14px;">Investment Update</p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:36px 40px;">
                            <h2 style="margin:0 0 6px;color:#ffffff;font-size:20px;">Hi %s,</h2>
                            <p style="margin:0 0 24px;color:#9ca3af;font-size:15px;">The founder has decided not to proceed with your investment at this time. A full refund has been initiated.</p>
                            <div style="background:#12152a;border:1px solid #7f1d1d;border-radius:12px;padding:20px 24px;margin-bottom:28px;">
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr><td style="color:#6b7280;font-size:13px;padding:6px 0;">Startup</td><td style="color:#ffffff;font-size:13px;font-weight:600;text-align:right;">%s</td></tr>
                                <tr><td style="color:#6b7280;font-size:13px;padding:6px 0;">Amount</td><td style="color:#f87171;font-size:15px;font-weight:700;text-align:right;">₹%s</td></tr>
                                <tr><td style="color:#6b7280;font-size:13px;padding:6px 0;">Status</td><td style="text-align:right;"><span style="background:#7f1d1d;color:#fca5a5;font-size:12px;font-weight:600;padding:3px 10px;border-radius:20px;">REJECTED — REFUND INITIATED</span></td></tr>
                              </table>
                            </div>
                            <p style="color:#9ca3af;font-size:14px;">Your refund of <strong style="color:#ffffff;">₹%s</strong> will reflect in your account within 5–7 business days.</p>
                            <p style="color:#6b7280;font-size:13px;margin-top:16px;">Browse other startups on FounderLink and find your next opportunity.</p>
                            <div style="text-align:center;margin-top:28px;">
                              <a href="http://localhost:3000/investor/startups" style="display:inline-block;background:linear-gradient(135deg,#4f46e5,#7c3aed);color:#fff;text-decoration:none;padding:13px 32px;border-radius:10px;font-size:14px;font-weight:600;">Browse Startups →</a>
                            </div>
                          </td>
                        </tr>
                        <tr><td style="padding:20px 40px;border-top:1px solid #2d3148;text-align:center;">
                          <p style="margin:0;color:#4b5563;font-size:12px;">FounderLink · Connect Build Grow Together</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                payment.getInvestorName(),
                payment.getStartupName(),
                String.format("%,.0f", payment.getAmount()),
                String.format("%,.0f", payment.getAmount())
        );
    }

    private String buildFounderSuccessHtml(Payment payment) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background-color:#0f1117;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#0f1117;padding:40px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" style="background-color:#1a1d27;border-radius:16px;overflow:hidden;border:1px solid #2d3148;">
                        <tr>
                          <td style="background:linear-gradient(135deg,#4f46e5,#7c3aed);padding:36px 40px;text-align:center;">
                            <h1 style="margin:0;color:#ffffff;font-size:26px;font-weight:700;">FounderLink</h1>
                            <p style="margin:6px 0 0;color:#c4b5fd;font-size:14px;">New Investment Received 💰</p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:36px 40px;">
                            <h2 style="margin:0 0 6px;color:#ffffff;font-size:20px;">Congratulations! 🎉</h2>
                            <p style="margin:0 0 24px;color:#9ca3af;font-size:15px;">Your startup <strong style="color:#ffffff;">%s</strong> just received a confirmed investment!</p>
                            <div style="background:#12152a;border:1px solid #2d3148;border-radius:12px;padding:20px 24px;margin-bottom:28px;">
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr><td style="color:#6b7280;font-size:13px;padding:6px 0;">Investor</td><td style="color:#ffffff;font-size:13px;font-weight:600;text-align:right;">%s</td></tr>
                                <tr><td style="color:#6b7280;font-size:13px;padding:6px 0;">Amount</td><td style="color:#10b981;font-size:15px;font-weight:700;text-align:right;">₹%s</td></tr>
                                <tr><td style="color:#6b7280;font-size:13px;padding:6px 0;">Payment ID</td><td style="color:#9ca3af;font-size:12px;font-family:monospace;text-align:right;">%s</td></tr>
                                <tr><td style="color:#6b7280;font-size:13px;padding:6px 0;">Status</td><td style="text-align:right;"><span style="background:#065f46;color:#6ee7b7;font-size:12px;font-weight:600;padding:3px 10px;border-radius:20px;">CONFIRMED</span></td></tr>
                              </table>
                            </div>
                            <p style="color:#9ca3af;font-size:14px;">Log in to FounderLink to view your updated investment dashboard and connect with your investor.</p>
                            <div style="text-align:center;margin-top:28px;">
                              <a href="http://localhost:3000/founder/investments" style="display:inline-block;background:linear-gradient(135deg,#4f46e5,#7c3aed);color:#fff;text-decoration:none;padding:13px 32px;border-radius:10px;font-size:14px;font-weight:600;">View Investments →</a>
                            </div>
                          </td>
                        </tr>
                        <tr><td style="padding:20px 40px;border-top:1px solid #2d3148;text-align:center;">
                          <p style="margin:0;color:#4b5563;font-size:12px;">FounderLink · Connect Build Grow Together</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                payment.getStartupName(),
                payment.getInvestorName(),
                String.format("%,.0f", payment.getAmount()),
                payment.getRazorpayPaymentId() != null ? payment.getRazorpayPaymentId() : "N/A"
        );
    }
}
