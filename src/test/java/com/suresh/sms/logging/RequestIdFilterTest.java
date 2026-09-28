package com.suresh.sms.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private static MockHttpServletRequest requestWithId(String id) {

        MockHttpServletRequest request = new MockHttpServletRequest();

        if (id != null) {
            request.addHeader("X-Request-Id", id);
        }

        return request;
    }


    @Test
    void generatesAnIdWhenNoneIsSupplied() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWithId(null), response, (req, res) -> { });

        String id = response.getHeader("X-Request-Id");

        assertNotNull(id);
        // throws IllegalArgumentException if it is not a UUID
        UUID.fromString(id);
    }

    @Test
    void twoRequestsGetDifferentGeneratedIds() throws Exception {

        MockHttpServletResponse first = new MockHttpServletResponse();
        MockHttpServletResponse second = new MockHttpServletResponse();

        filter.doFilter(requestWithId(null), first, (req, res) -> { });
        filter.doFilter(requestWithId(null), second, (req, res) -> { });

        assertNotEquals(first.getHeader("X-Request-Id"), second.getHeader("X-Request-Id"));
    }

    @Test
    void reusesAValidIdSuppliedByTheCaller() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWithId("trace-abc.123_X"), response, (req, res) -> { });

        assertEquals("trace-abc.123_X", response.getHeader("X-Request-Id"));
    }

    @Test
    void replacesSuppliedIdsThatCouldForgeLogLines() throws Exception {

        List<String> unsafe = List.of(
                "has space",
                "bad\r\nForged-Log-Line: hello",
                "",
                "a".repeat(65),
                "semi;colon",
                "<script>alert(1)</script>");

        for (String value : unsafe) {

            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(requestWithId(value), response, (req, res) -> { });

            String id = response.getHeader("X-Request-Id");

            assertNotNull(id, "Header must be set for input: " + value);
            assertNotEquals(value, id, "Unsafe value must not be echoed: " + value);
            // the replacement is a generated UUID
            UUID.fromString(id);
        }
    }

    @Test
    void acceptsAnIdOfExactlyTheMaximumLength() throws Exception {

        String maxLength = "a".repeat(64);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWithId(maxLength), response, (req, res) -> { });

        assertEquals(maxLength, response.getHeader("X-Request-Id"));
    }

    @Test
    void idIsInTheMdcWhileTheRequestRunsAndRemovedAfterwards() throws Exception {

        AtomicReference<String> seenInsideTheRequest = new AtomicReference<>();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                requestWithId("trace-1"),
                response,
                (req, res) -> seenInsideTheRequest.set(MDC.get("requestId")));

        assertEquals("trace-1", seenInsideTheRequest.get());
        assertNull(MDC.get("requestId"), "MDC must be cleaned up after the request");
    }

    @Test
    void mdcIsCleanedUpEvenWhenTheRequestFails() {

        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(ServletException.class, () ->
                filter.doFilter(requestWithId("trace-2"), response, (req, res) -> {
                    throw new ServletException("boom");
                }));

        assertNull(MDC.get("requestId"));
    }
}
