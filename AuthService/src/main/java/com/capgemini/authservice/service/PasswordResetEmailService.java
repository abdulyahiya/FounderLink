package com.capgemini.authservice.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendResetLink(String toEmail, String name, String token) {
        String resetLink = "http://localhost:3001/reset-password?token=" + token;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset Your FounderLink Password");
            helper.setText(buildHtml(name, resetLink), true);

            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildHtml(String name, String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
                  <title>Reset Your Password</title>
                </head>
                <body style="margin:0;padding:0;background-color:#0f1117;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="background-color:#0f1117;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background-color:#1a1d27;border-radius:16px;
                                      overflow:hidden;border:1px solid #2d3148;
                                      max-width:600px;width:100%%;">
                                      
                          <!-- ── HEADER ── -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#ef4444 0%%,#b91c1c 100%%);
                                        padding:40px 40px 32px;text-align:center;">
                              <div style="display:inline-block;background:rgba(255,255,255,0.15);
                                          border-radius:12px;padding:10px 20px;margin-bottom:16px;">
                                <span style="color:#ffffff;font-size:18px;font-weight:800;
                                             letter-spacing:1px;">FL</span>
                              </div>
                              <h1 style="margin:0;color:#ffffff;font-size:28px;
                                          font-weight:700;letter-spacing:-0.5px;">
                                Password Reset
                              </h1>
                            </td>
                          </tr>

                          <!-- ── BODY ── -->
                          <tr>
                            <td style="padding:40px;">
                              <h2 style="margin:0 0 16px;color:#ffffff;font-size:22px;font-weight:600;">
                                Hello %s,
                              </h2>
                              <p style="margin:0 0 24px;color:#9ca3af;font-size:15px;line-height:1.7;">
                                We received a request to reset the password for your FounderLink account. 
                                Click the button below to choose a new password. This link will expire in 1 hour.
                              </p>

                              <!-- CTA button -->
                              <div style="text-align:center;margin-top:32px;margin-bottom:32px;">
                                <a href="%s"
                                   style="display:inline-block;
                                          background:linear-gradient(135deg,#ef4444,#b91c1c);
                                          color:#ffffff;text-decoration:none;
                                          padding:14px 40px;border-radius:10px;
                                          font-size:15px;font-weight:600;
                                          letter-spacing:0.3px;">
                                  Reset Password &rarr;
                                </a>
                              </div>

                              <p style="margin:0;color:#9ca3af;font-size:14px;line-height:1.6;">
                                If you did not request a password reset, you can safely ignore this email. 
                                Your password will remain unchanged.
                              </p>
                            </td>
                          </tr>

                          <!-- ── FOOTER ── -->
                          <tr>
                            <td style="padding:24px 40px;border-top:1px solid #2d3148;
                                        text-align:center;">
                              <p style="margin:0;color:#4b5563;font-size:12px;line-height:1.6;">
                                &copy; 2026 FounderLink. All rights reserved.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(name, resetLink);
    }
}
