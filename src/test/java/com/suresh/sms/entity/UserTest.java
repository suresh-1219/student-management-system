package com.suresh.sms.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void testUserGettersAndSetters() {

        User user = new User();

        user.setId(1L);
        user.setUsername("suresh");
        user.setEmail("suresh@gmail.com");
        user.setPassword("password");
        user.setRole(Role.USER);

        assertEquals(1L, user.getId());
        assertEquals("suresh", user.getUsername());
        assertEquals("suresh@gmail.com", user.getEmail());
        assertEquals("password", user.getPassword());
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void testUserConstructor() {

        User user = new User();

        assertNotNull(user);
    }
}