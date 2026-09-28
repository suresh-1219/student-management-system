package com.suresh.sms.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Gives every request an ID that appears on every log line it produces (via
 * the SLF4J MDC) and is sent back to the caller in the {@code X-Request-Id}
 * response header, so a user can quote it and you can find that exact
 * request in the logs.
 *
 * <p>A caller (or a load balancer) may supply its own ID so one ID can follow
 * a request across several services - but only if it is short and made of
 * harmless characters. Anything else is discarded and replaced: the value
 * ends up in log files, and a newline in it would let a caller forge extra
 * log lines ("log injection").
 *
 * <p>Runs before everything else - including Spring Security - so even
 * rejected (401/403/429) requests are traceable.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestId = resolve(request.getHeader(HEADER));

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Threads are reused for later requests: never leave an ID behind.
            MDC.remove(MDC_KEY);
        }
    }

    static String resolve(String supplied) {

        if (supplied != null && SAFE_ID.matcher(supplied).matches()) {
            return supplied;
        }

        return UUID.randomUUID().toString();
    }
}
