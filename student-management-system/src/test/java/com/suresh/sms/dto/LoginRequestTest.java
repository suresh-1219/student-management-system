package com.suresh.sms.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class LoginRequestTest {

    @Test
    void testDefaultConstructor() {
        LoginRequest request = new LoginRequest();

        assertNotNull(request);
    }

    @Test
    void testParameterizedConstructor() {
        LoginRequest request = new LoginRequest("suresh", "password");

        assertEquals("suresh", request.getUsername());
        assertEquals("password", request.getPassword());
    }

    @Test
    void testGettersAndSetters() {
        LoginRequest request = new LoginRequest();

        request.setUsername("admin");
        request.setPassword("admin123");

        assertEquals("admin", request.getUsername());
        assertEquals("admin123", request.getPassword());
    }
}