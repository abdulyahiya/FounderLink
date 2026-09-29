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
public class WelcomeEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendWelcome(String toEmail, String name, String role) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to FounderLink, " + name + "!");
            helper.setText(buildHtml(name, role), true);

            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildHtml(String name, String role) {
        String roleLabel       = roleLabel(role);
        String roleDescription = roleDescription(role);
        String accentColor     = roleColor(role);
        String nextSteps       = nextSteps(role);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
                  <title>Welcome to FounderLink</title>
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
                            <td style="background:linear-gradient(135deg,#4f46e5 0%%,#7c3aed 100%%);
                                        padding:40px 40px 32px;text-align:center;">
                              <div style="display:inline-block;background:rgba(255,255,255,0.15);
                                          border-radius:12px;padding:10px 20px;margin-bottom:16px;">
                                <span style="color:#ffffff;font-size:18px;font-weight:800;
                                             letter-spacing:1px;">FL</span>
                              </div>
                              <h1 style="margin:0;color:#ffffff;font-size:28px;
                                          font-weight:700;letter-spacing:-0.5px;">
                                FounderLink
                              </h1>
                              <p style="margin:8px 0 0;color:#c4b5fd;font-size:14px;">
                                Connect · Build · Grow Together
                              </p>
                            </td>
                          </tr>

                          <!-- ── BODY ── -->
                          <tr>
                            <td style="padding:40px;">

                              <h2 style="margin:0 0 8px;color:#ffffff;font-size:22px;font-weight:600;">
                                Welcome aboard, %s!
                              </h2>
                              <p style="margin:0 0 28px;color:#9ca3af;font-size:15px;line-height:1.7;">
                                Your account has been successfully created on FounderLink.
                                Here's everything you need to get started.
                              </p>

                              <!-- Role card -->
                              <div style="background:#12152a;border:1px solid %s;
                                          border-radius:12px;padding:20px 24px;margin-bottom:28px;">
                                <p style="margin:0 0 6px;color:#6b7280;font-size:11px;
                                           text-transform:uppercase;letter-spacing:1.5px;
                                           font-weight:600;">
                                  Your Profile Type
                                </p>
                                <p style="margin:0;color:%s;font-size:22px;font-weight:700;">
                                  %s
                                </p>
                                <p style="margin:10px 0 0;color:#9ca3af;font-size:14px;
                                           line-height:1.6;">
                                  %s
                                </p>
                              </div>

                              <!-- Next steps -->
                              <p style="margin:0 0 14px;color:#ffffff;font-size:15px;font-weight:600;">
                                What you can do next:
                              </p>
                              %s

                              <!-- CTA button -->
                              <div style="text-align:center;margin-top:36px;">
                                <a href="http://localhost:3000"
                                   style="display:inline-block;
                                          background:linear-gradient(135deg,#4f46e5,#7c3aed);
                                          color:#ffffff;text-decoration:none;
                                          padding:14px 40px;border-radius:10px;
                                          font-size:15px;font-weight:600;
                                          letter-spacing:0.3px;">
                                  Go to FounderLink &rarr;
                                </a>
                              </div>

                            </td>
                          </tr>

                          <!-- ── FOOTER ── -->
                          <tr>
                            <td style="padding:24px 40px;border-top:1px solid #2d3148;
                                        text-align:center;">
                              <p style="margin:0;color:#4b5563;font-size:12px;line-height:1.6;">
                                You received this email because you created an account on
                                FounderLink.<br/>
                                If you did not register, please ignore this email.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                name,
                accentColor, accentColor, roleLabel, roleDescription,
                nextSteps
        );
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String roleLabel(String role) {
        return switch (role) {
            case "ROLE_FOUNDER"   -> "Founder";
            case "ROLE_INVESTOR"  -> "Investor";
            case "ROLE_COFOUNDER" -> "Co-Founder";
            default               -> "Member";
        };
    }

    private String roleDescription(String role) {
        return switch (role) {
            case "ROLE_FOUNDER"   ->
                    "You're set up as a Founder. Create your startup, build your team, and connect with the right investors.";
            case "ROLE_INVESTOR"  ->
                    "You're set up as an Investor. Discover curated startups and invest securely through Razorpay.";
            case "ROLE_COFOUNDER" ->
                    "You're set up as a Co-Founder. Accept team invitations and collaborate to build something amazing.";
            default -> "Your FounderLink profile is ready to use.";
        };
    }

    private String roleColor(String role) {
        return switch (role) {
            case "ROLE_FOUNDER"   -> "#6366f1";
            case "ROLE_INVESTOR"  -> "#10b981";
            case "ROLE_COFOUNDER" -> "#a78bfa";
            default               -> "#6366f1";
        };
    }

    private String nextSteps(String role) {
        String[] steps = switch (role) {
            case "ROLE_FOUNDER" -> new String[]{
                "Complete your profile with bio and skills",
                "Create your first startup listing",
                "Invite co-founders to join your team",
                "Track investment requests from investors"
            };
            case "ROLE_INVESTOR" -> new String[]{
                "Complete your profile",
                "Browse admin-approved startup listings",
                "Invest securely via Razorpay",
                "Track your portfolio and payment history"
            };
            case "ROLE_COFOUNDER" -> new String[]{
                "Complete your profile with your skills",
                "Wait for team invitations from founders",
                "Accept invitations to join startup teams",
                "Collaborate and help build great products"
            };
            default -> new String[]{"Complete your profile", "Explore the platform"};
        };

        StringBuilder sb = new StringBuilder(
                "<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%%\">"
        );
        for (String step : steps) {
            sb.append("""
                    <tr>
                      <td width="28" valign="top"
                          style="padding:7px 0;color:#6366f1;font-size:16px;font-weight:700;">
                        &#10003;
                      </td>
                      <td style="padding:7px 0;color:#d1d5db;font-size:14px;line-height:1.5;">
                        %s
                      </td>
                    </tr>
                    """.formatted(step));
        }
        sb.append("</table>");
        return sb.toString();
    }
}
