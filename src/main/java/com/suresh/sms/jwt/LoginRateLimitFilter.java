package com.suresh.sms.jwt;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A simple in-memory sliding-window rate limiter for {@code POST /auth/login},
 * to slow down password-guessing. At most {@value #MAX_ATTEMPTS} attempts per
 * client IP per {@value #WINDOW_MS}ms window; further attempts get
 * {@code 429 Too Many Requests}.
 *
 * <p>This is per application instance. Running more than one instance behind
 * a load balancer would need a shared store (Redis, say) for the limit to
 * apply across all of them - out of scope for this project's current scale.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, Deque<Long>> attemptsByIp = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (isLoginRequest(request) && isRateLimited(clientIp(request))) {

            writeTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {

        return "POST".equalsIgnoreCase(request.getMethod())
                && "/auth/login".equals(request.getRequestURI());
    }

    private boolean isRateLimited(String ip) {

        Deque<Long> timestamps = attemptsByIp.computeIfAbsent(
                ip, key -> new ConcurrentLinkedDeque<>());

        long now = System.currentTimeMillis();

        synchronized (timestamps) {

            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= MAX_ATTEMPTS) {
                return true;
            }

            timestamps.addLast(now);
            return false;
        }
    }

    private String clientIp(HttpServletRequest request) {

        // Trusts X-Forwarded-For only because this app is expected to sit
        // behind a single reverse proxy that sets it; a public-facing
        // deployment with untrusted intermediaries should not trust this
        // header without further validation.
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\","
                        + "\"message\":\"Too many login attempts. Please try again later.\"}",
                Instant.now()));
    }
}
