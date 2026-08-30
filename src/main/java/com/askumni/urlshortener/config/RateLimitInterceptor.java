package com.askumni.urlshortener.config;

import com.askumni.urlshortener.exception.RateLimitExceededException;
import com.askumni.urlshortener.service.TokenBucketRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final TokenBucketRateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientKey = resolveClientKey(request);

        if (!rateLimiter.tryConsume(clientKey)) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded for client: " + clientKey + ". Please slow down and try again shortly.");
        }
        return true;
    }

    // In production, prefer an authenticated API key over IP when available,
    // since IPs can be shared (NAT, corporate networks) or spoofed via headers.
    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
