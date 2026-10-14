package com.capgemini.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@Slf4j
public class ClientIpResolver {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:))$");

    private final boolean trustCloudflareHeader;

    public ClientIpResolver(@Value("${app.security.trust-cloudflare:false}") boolean trustCloudflareHeader) {
        this.trustCloudflareHeader = trustCloudflareHeader;
    }

    /**
     * Resolves the real client IP address safely behind reverse proxies (Render, Nginx).
     *
     * Security & Spoofing Defense:
     * 1. CF-Connecting-IP: Direct clients connecting to Render can forge arbitrary headers.
     *    Therefore, CF-Connecting-IP is NOT trusted by default unless app.security.trust-cloudflare=true.
     * 2. X-Forwarded-For: Format is 'client, proxy1, proxy2'. An attacker can send 'X-Forwarded-For: 1.1.1.1'.
     *    When Render's reverse proxy receives the request, it appends the client's actual IP to the END
     *    of the chain ('1.1.1.1, actual_ip'). Reading the leftmost entry (index 0) allows trivial spoofing.
     *    Therefore, the trusted client IP is the rightmost valid entry added by the immediate reverse proxy.
     * 3. IP Validation: All resolved values are strictly validated against IPv4/IPv6 patterns to prevent
     *    header injection or corrupt Redis keys.
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // 1. Cloudflare connecting IP (only when explicitly enabled via configuration)
        if (trustCloudflareHeader) {
            String cfIp = cleanIp(request.getHeader("CF-Connecting-IP"));
            if (isValidIp(cfIp)) {
                return cfIp;
            }
        }

        // 2. X-Forwarded-For: Read from the rightmost (last) entry appended by the trusted proxy
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] parts = xForwardedFor.split(",");
            // First pass: find the rightmost public/non-internal IP
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = cleanIp(parts[i]);
                if (isValidIp(candidate) && !isInternalOrLoopback(candidate)) {
                    return candidate;
                }
            }
            // Second pass: if all entries are internal/loopback (e.g. dev/test), pick the rightmost valid IP
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = cleanIp(parts[i]);
                if (isValidIp(candidate)) {
                    return candidate;
                }
            }
        }

        // 3. Fallback to direct remote address
        String remoteAddr = cleanIp(request.getRemoteAddr());
        if (isValidIp(remoteAddr)) {
            return remoteAddr;
        }

        return "unknown";
    }

    public boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches();
    }

    private String cleanIp(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim();
        // Strip enclosing brackets from IPv6: [2001:db8::1] -> 2001:db8::1
        if (cleaned.startsWith("[") && cleaned.contains("]")) {
            int closeBracket = cleaned.indexOf(']');
            cleaned = cleaned.substring(1, closeBracket);
        } else if (cleaned.contains(":") && !cleaned.contains("::")) {
            // IPv4 with port: 192.168.1.1:8080 -> 192.168.1.1
            int colonIndex = cleaned.lastIndexOf(':');
            if (colonIndex > 0 && cleaned.indexOf(':') == colonIndex) {
                cleaned = cleaned.substring(0, colonIndex);
            }
        }
        return cleaned.trim();
    }

    private boolean isInternalOrLoopback(String ip) {
        if (ip == null) return true;
        return ip.equals("127.0.0.1")
                || ip.equals("::1")
                || ip.equals("0:0:0:0:0:0:0:1")
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || (ip.startsWith("172.") && is172Private(ip));
    }

    private boolean is172Private(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                int secondOctet = Integer.parseInt(parts[1]);
                return secondOctet >= 16 && secondOctet <= 31;
            }
        } catch (NumberFormatException ignored) {}
        return false;
    }
}
