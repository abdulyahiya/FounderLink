package com.capgemini.ratelimit;

import lombok.Getter;

@Getter
public enum RateLimitPolicy {
    LOGIN("login", 5, 60),                      // 5 requests per 1 minute
    REGISTER("register", 5, 60),                // 5 requests per 1 minute
    FORGOT_PASSWORD("forgot-password", 3, 900), // 3 requests per 15 minutes
    RESET_PASSWORD("reset-password", 5, 900),   // 5 requests per 15 minutes
    CHANGE_PASSWORD("change-password", 5, 900), // 5 requests per 15 minutes
    OTP("otp", 5, 60),                          // 5 requests per 1 minute
    REFRESH_TOKEN("refresh-token", 30, 60),     // 30 requests per 1 minute
    PAYMENT("payment", 10, 60),                 // 10 requests per 1 minute
    GLOBAL("global", 120, 60);                  // 120 requests per 1 minute for general endpoints

    private final String keyPrefix;
    private final long maxRequests;
    private final long windowSeconds;

    RateLimitPolicy(String keyPrefix, long maxRequests, long windowSeconds) {
        this.keyPrefix = keyPrefix;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public static RateLimitPolicy resolve(String uri, String method) {
        if (uri == null || method == null) {
            return GLOBAL;
        }

        // Normalize URI: strip query string, strip trailing slashes, convert to lowercase
        String path = uri.trim();
        int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            path = path.substring(0, queryIndex);
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        path = path.toLowerCase();

        if ("POST".equalsIgnoreCase(method)) {
            if (isExactOrPrefixed(path, "/auth/login")) {
                return LOGIN;
            }
            if (isExactOrPrefixed(path, "/auth/register")) {
                return REGISTER;
            }
            if (isExactOrPrefixed(path, "/auth/forgot-password")) {
                return FORGOT_PASSWORD;
            }
            if (isExactOrPrefixed(path, "/auth/reset-password")) {
                return RESET_PASSWORD;
            }
            if (isExactOrPrefixed(path, "/auth/change-password")) {
                return CHANGE_PASSWORD;
            }
            if (isExactOrPrefixed(path, "/auth/refresh") || isExactOrPrefixed(path, "/auth/refresh-token")) {
                return REFRESH_TOKEN;
            }
            if (isExactOrPrefixed(path, "/auth/otp") || isExactOrPrefixed(path, "/auth/verify-otp") || isExactOrPrefixed(path, "/auth/send-otp")) {
                return OTP;
            }
            if (path.contains("/payments") || path.contains("/api/payments")) {
                return PAYMENT;
            }
        }
        return GLOBAL;
    }

    private static boolean isExactOrPrefixed(String path, String target) {
        return path.equals(target)
                || path.endsWith(target)
                || path.equals("/api" + target)
                || path.endsWith("/api" + target);
    }
}
