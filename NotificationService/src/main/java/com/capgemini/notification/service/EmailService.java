package com.capgemini.notification.service;

import jakarta.mail.MessagingException;
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
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendWelcomeEmail(String toEmail, String name, String role) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to FounderLink, " + name + "!");
            helper.setText(buildWelcomeEmailHtml(name, role), true);

            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildWelcomeEmailHtml(String name, String role) {
        String roleLabel = getRoleLabel(role);
        String roleDescription = getRoleDescription(role);
        String nextSteps = getNextSteps(role);
        String accentColor = getRoleColor(role);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>Welcome to FounderLink</title>
                </head>
                <body style="margin:0;padding:0;background-color:#0f1117;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#0f1117;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#1a1d27;border-radius:16px;overflow:hidden;border:1px solid #2d3148;">

                          <!-- Header -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#4f46e5 0%%,#7c3aed 100%%);padding:40px 40px 30px;text-align:center;">
                              <h1 style="margin:0;color:#ffffff;font-size:28px;font-weight:700;letter-spacing:-0.5px;">FounderLink</h1>
                              <p style="margin:8px 0 0;color:#c4b5fd;font-size:14px;">Connect · Build · Grow Together</p>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:40px;">

                              <!-- Greeting -->
                              <h2 style="margin:0 0 8px;color:#ffffff;font-size:22px;font-weight:600;">
                                Welcome aboard, %s! 👋
                              </h2>
                              <p style="margin:0 0 24px;color:#9ca3af;font-size:15px;line-height:1.6;">
                                Your account has been successfully created on FounderLink.
                              </p>

                              <!-- Role badge -->
                              <div style="background-color:#12152a;border:1px solid %s;border-radius:12px;padding:20px 24px;margin-bottom:28px;">
                                <p style="margin:0 0 4px;color:#6b7280;font-size:12px;text-transform:uppercase;letter-spacing:1px;font-weight:600;">Your Profile</p>
                                <p style="margin:0;color:#ffffff;font-size:20px;font-weight:700;">%s</p>
                                <p style="margin:8px 0 0;color:#9ca3af;font-size:14px;line-height:1.5;">%s</p>
                              </div>

                              <!-- What's next -->
                              <h3 style="margin:0 0 16px;color:#ffffff;font-size:16px;font-weight:600;">What you can do next:</h3>
                              %s

                              <!-- CTA -->
                              <div style="text-align:center;margin-top:32px;">
                                <a href="http://localhost:3000" style="display:inline-block;background:linear-gradient(135deg,#4f46e5,#7c3aed);color:#ffffff;text-decoration:none;padding:14px 36px;border-radius:10px;font-size:15px;font-weight:600;letter-spacing:0.3px;">
                                  Go to FounderLink →
                                </a>
                              </div>

                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="padding:24px 40px;border-top:1px solid #2d3148;text-align:center;">
                              <p style="margin:0;color:#4b5563;font-size:12px;line-height:1.6;">
                                This email was sent because you registered on FounderLink.<br/>
                                If you did not create this account, please ignore this email.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(name, accentColor, roleLabel, roleDescription, nextSteps);
    }

    private String getRoleLabel(String role) {
        return switch (role) {
            case "ROLE_FOUNDER"   -> "Founder";
            case "ROLE_INVESTOR"  -> "Investor";
            case "ROLE_COFOUNDER" -> "Co-Founder";
            default               -> "Member";
        };
    }

    private String getRoleDescription(String role) {
        return switch (role) {
            case "ROLE_FOUNDER"   -> "You're set up as a Founder. Build your startup, manage your team, and connect with investors.";
            case "ROLE_INVESTOR"  -> "You're set up as an Investor. Browse curated startups and invest securely via Razorpay.";
            case "ROLE_COFOUNDER" -> "You're set up as a Co-Founder. Join startup teams and collaborate to build something great.";
            default               -> "Your FounderLink profile is ready.";
        };
    }

    private String getNextSteps(String role) {
        return switch (role) {
            case "ROLE_FOUNDER" -> buildStepsList(new String[]{
                    "Complete your profile",
                    "Create your first startup listing",
                    "Invite co-founders to your team",
                    "Track investment requests from investors"
            });
            case "ROLE_INVESTOR" -> buildStepsList(new String[]{
                    "Complete your profile",
                    "Browse approved startups",
                    "Invest securely via Razorpay",
                    "Track your portfolio and payment history"
            });
            case "ROLE_COFOUNDER" -> buildStepsList(new String[]{
                    "Complete your profile",
                    "Accept team invitations from founders",
                    "Collaborate on startup development",
                    "Grow with your team"
            });
            default -> buildStepsList(new String[]{"Complete your profile", "Explore the platform"});
        };
    }

    private String buildStepsList(String[] steps) {
        StringBuilder sb = new StringBuilder("<ul style=\"margin:0;padding:0;list-style:none;\">");
        for (String step : steps) {
            sb.append("""
                    <li style="display:flex;align-items:center;gap:10px;padding:8px 0;color:#d1d5db;font-size:14px;">
                      <span style="color:#4f46e5;font-size:16px;flex-shrink:0;">✓</span> %s
                    </li>
                    """.formatted(step));
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private String getRoleColor(String role) {
        return switch (role) {
            case "ROLE_FOUNDER"   -> "#4f46e5";
            case "ROLE_INVESTOR"  -> "#059669";
            case "ROLE_COFOUNDER" -> "#7c3aed";
            default               -> "#4f46e5";
        };
    }
}
