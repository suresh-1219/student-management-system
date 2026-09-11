package com.suresh.sms.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void testRoleValues() {

        assertNotNull(Role.ADMIN);
        assertNotNull(Role.USER);

        assertEquals("ADMIN", Role.ADMIN.name());
        assertEquals("USER", Role.USER.name());
    }

    @Test
    void testRoleValuesMethod() {

        Role[] roles = Role.values();

        assertEquals(2, roles.length);
        assertEquals(Role.ADMIN, roles[0]);
        assertEquals(Role.USER, roles[1]);
    }
}