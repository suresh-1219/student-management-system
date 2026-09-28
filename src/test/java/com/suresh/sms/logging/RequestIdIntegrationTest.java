package com.suresh.sms.logging;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.suresh.sms.support.AbstractIntegrationTest;

/**
 * Proves the filter is registered in the real application, not just correct
 * in isolation (see RequestIdFilterTest for the detailed behavior).
 *
 * Uses /users/register rather than /auth/login: login is rate limited per IP
 * and every test in this JVM shares one client address.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RequestIdIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    void everyResponseCarriesARequestId() throws Exception {

        mockMvc.perform(post("/users/register"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void aCallerSuppliedRequestIdIsEchoedBack() throws Exception {

        mockMvc.perform(
                post("/users/register")
                        .header("X-Request-Id", "my-trace-123")
        )
        .andExpect(header().string("X-Request-Id", "my-trace-123"));
    }
}
