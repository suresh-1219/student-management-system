package com.suresh.sms.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

/**
 * Unit tests for {@link LoginRateLimitFilter}'s sliding-window logic, run
 * directly against the filter (no Spring context needed).
 */
class LoginRateLimitFilterTest {

    private final LoginRateLimitFilter filter = new LoginRateLimitFilter();

    private MockHttpServletRequest loginRequest(String ip) {

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    void allowsUpToFiveAttemptsThenBlocksTheSixth() throws Exception {

        String ip = "10.0.0.1";
        FilterChain chain = mock(FilterChain.class);

        for (int i = 1; i <= 5; i++) {

            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(loginRequest(ip), response, chain);

            assertEquals(200, response.getStatus(),
                    "Attempt " + i + " should be allowed through");
        }

        verify(chain, times(5)).doFilter(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        MockHttpServletResponse sixth = new MockHttpServletResponse();
        filter.doFilter(loginRequest(ip), sixth, chain);

        assertEquals(429, sixth.getStatus());
        // MockHttpServletResponse.getContentType() includes the character
        // encoding once it's set (see writeTooManyRequests), so the full
        // value is "application/json;charset=UTF-8" - startsWith avoids
        // hard-coding that suffix here.
        assertTrue(sixth.getContentType().startsWith("application/json"));
        // The filter chain must NOT have been invoked a 6th time.
        verify(chain, times(5)).doFilter(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void tracksEachIpAddressIndependently() throws Exception {

        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequest("10.0.0.2"), new MockHttpServletResponse(), chain);
        }

        // A different IP starts with a fresh count and should not be blocked.
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.3"), response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void doesNotRateLimitRequestsToOtherEndpoints() throws Exception {

        FilterChain chain = mock(FilterChain.class);
        String ip = "10.0.0.4";

        for (int i = 0; i < 10; i++) {

            MockHttpServletRequest request =
                    new MockHttpServletRequest("GET", "/students");
            request.setRemoteAddr(ip);

            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);

            assertEquals(200, response.getStatus(),
                    "Non-login requests must never be rate limited");
        }
    }

    @Test
    void honorsXForwardedForOverTheRawRemoteAddress() throws Exception {

        FilterChain chain = mock(FilterChain.class);

        // Same underlying connection (proxy), different declared client IPs -
        // each X-Forwarded-For value gets its own bucket.
        for (int i = 0; i < 5; i++) {

            MockHttpServletRequest request = loginRequest("192.168.1.1");
            request.addHeader("X-Forwarded-For", "203.0.113.5");

            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest blocked = loginRequest("192.168.1.1");
        blocked.addHeader("X-Forwarded-For", "203.0.113.5");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(blocked, blockedResponse, chain);

        assertEquals(429, blockedResponse.getStatus());

        MockHttpServletRequest differentClient = loginRequest("192.168.1.1");
        differentClient.addHeader("X-Forwarded-For", "203.0.113.9");
        MockHttpServletResponse allowedResponse = new MockHttpServletResponse();
        filter.doFilter(differentClient, allowedResponse, chain);

        assertEquals(200, allowedResponse.getStatus());
    }
}
