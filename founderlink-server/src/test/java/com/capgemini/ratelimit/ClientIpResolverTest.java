package com.capgemini.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private ClientIpResolver defaultResolver;
    private ClientIpResolver cfTrustedResolver;

    @BeforeEach
    void setUp() {
        defaultResolver = new ClientIpResolver(false);
        cfTrustedResolver = new ClientIpResolver(true);
    }

    @Test
    void resolveClientIp_whenAttackerSpoofsXForwardedFor_shouldPickRightmostTrustedProxyIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // Attacker sent 'X-Forwarded-For: 1.1.1.1', Render appended their real IP '198.51.100.50'
        request.addHeader("X-Forwarded-For", "1.1.1.1, 198.51.100.50");
        request.setRemoteAddr("10.0.0.2");

        String ip = defaultResolver.resolveClientIp(request);

        // Security check: attacker's spoofed 1.1.1.1 must NOT be trusted; the real IP added by Render must be used
        assertThat(ip).isEqualTo("198.51.100.50");
    }

    @Test
    void resolveClientIp_whenAttackerSpoofsCFConnectingIp_shouldIgnoreUnlessConfigured() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "8.8.8.8"); // Attacker forged header
        request.addHeader("X-Forwarded-For", "198.51.100.77"); // Added by Render
        request.setRemoteAddr("10.0.0.2");

        // By default, CF-Connecting-IP is untrusted
        String ipDefault = defaultResolver.resolveClientIp(request);
        assertThat(ipDefault).isEqualTo("198.51.100.77");

        // When explicitly trusted via configuration
        String ipTrusted = cfTrustedResolver.resolveClientIp(request);
        assertThat(ipTrusted).isEqualTo("8.8.8.8");
    }

    @Test
    void resolveClientIp_whenIpIncludesPort_shouldStripPortCleanly() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.195:44321");
        request.setRemoteAddr("10.0.0.2");

        String ip = defaultResolver.resolveClientIp(request);

        assertThat(ip).isEqualTo("203.0.113.195");
    }

    @Test
    void resolveClientIp_whenIpv6InBracketsWithPort_shouldStripBracketsAndPort() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "[2001:db8::1]:8080");
        request.setRemoteAddr("10.0.0.2");

        String ip = defaultResolver.resolveClientIp(request);

        assertThat(ip).isEqualTo("2001:db8::1");
    }

    @Test
    void resolveClientIp_whenXForwardedForContainsInvalidGarbage_shouldFallbackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "garbage_injection_header, <script>alert(1)</script>");
        request.setRemoteAddr("203.0.113.88");

        String ip = defaultResolver.resolveClientIp(request);

        assertThat(ip).isEqualTo("203.0.113.88");
    }

    @Test
    void resolveClientIp_whenAllEntriesInternalInDev_shouldReturnRightmostValidInternalIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "127.0.0.1, 10.0.0.5");
        request.setRemoteAddr("10.0.0.5");

        String ip = defaultResolver.resolveClientIp(request);

        assertThat(ip).isEqualTo("10.0.0.5");
    }

    @Test
    void resolveClientIp_whenNoProxyHeaders_shouldFallbackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        String ip = defaultResolver.resolveClientIp(request);

        assertThat(ip).isEqualTo("192.168.1.100");
    }

    @Test
    void isValidIp_shouldCorrectlyDifferentiateValidAndInvalidIps() {
        assertThat(defaultResolver.isValidIp("192.168.1.1")).isTrue();
        assertThat(defaultResolver.isValidIp("203.0.113.195")).isTrue();
        assertThat(defaultResolver.isValidIp("2001:db8::1")).isTrue();
        assertThat(defaultResolver.isValidIp("::1")).isTrue();

        assertThat(defaultResolver.isValidIp(null)).isFalse();
        assertThat(defaultResolver.isValidIp("")).isFalse();
        assertThat(defaultResolver.isValidIp("unknown")).isFalse();
        assertThat(defaultResolver.isValidIp("999.999.999.999")).isFalse();
        assertThat(defaultResolver.isValidIp("1.2.3.4.5")).isFalse();
        assertThat(defaultResolver.isValidIp("192.168.1.1; DROP TABLE users")).isFalse();
    }
}
