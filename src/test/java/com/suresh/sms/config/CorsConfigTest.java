package com.suresh.sms.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.suresh.sms.support.AbstractIntegrationTest;

/**
 * Uses the default allowed origins from application.properties
 * (http://localhost:3000 and http://localhost:5173).
 *
 * The "actual request" test deliberately uses /users/register, not
 * /auth/login: login is rate limited per IP, and every test in this JVM
 * shares one client address.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    void preflightFromAnAllowedOriginIsAccepted() throws Exception {

        mockMvc.perform(
                options("/students")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization")
        )
        .andExpect(status().isOk())
        .andExpect(header().string(
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
        .andExpect(header().string(
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")));
    }

    @Test
    void preflightFromAnUnknownOriginIsRejected() throws Exception {

        mockMvc.perform(
                options("/students")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        )
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void actualRequestFromAnAllowedOriginGetsCorsHeaders() throws Exception {

        mockMvc.perform(
                post("/users/register")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
        )
        .andExpect(status().isBadRequest())
        .andExpect(header().string(
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
        .andExpect(header().string(
                HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("X-Request-Id")));
    }

    @Test
    void actualRequestFromAnUnknownOriginIsRejected() throws Exception {

        mockMvc.perform(
                post("/users/register")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
        )
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
