package com.suresh.sms.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class LoginResponseTest {

    @Test
    void testDefaultConstructor() {
        LoginResponse response = new LoginResponse();

        assertNotNull(response);
    }

    @Test
    void testParameterizedConstructor() {
        LoginResponse response = new LoginResponse("test-token");

        assertEquals("test-token", response.getToken());
    }

    @Test
    void testGetterAndSetter() {
        LoginResponse response = new LoginResponse();

        response.setToken("abc123");

        assertEquals("abc123", response.getToken());
    }
}