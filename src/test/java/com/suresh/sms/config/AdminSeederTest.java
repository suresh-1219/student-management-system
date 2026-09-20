package com.suresh.sms.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.suresh.sms.dto.RegisterRequest;
import com.suresh.sms.entity.Role;
import com.suresh.sms.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserService userService;

    @Test
    void seedsAdminWhenPasswordProvidedAndUserMissing() {

        when(userService.usernameExists("admin")).thenReturn(false);

        AdminSeeder seeder = new AdminSeeder(
                userService, "admin", "admin@example.com", "StrongPass123");

        seeder.run(null);

        ArgumentCaptor<RegisterRequest> captor =
                ArgumentCaptor.forClass(RegisterRequest.class);

        verify(userService).createUser(captor.capture(), eq(Role.ADMIN));

        assertEquals("admin", captor.getValue().username());
        assertEquals("admin@example.com", captor.getValue().email());
    }

    @Test
    void skipsWhenPasswordNotSet() {

        AdminSeeder seeder = new AdminSeeder(
                userService, "admin", "admin@example.com", "");

        seeder.run(null);

        verify(userService, never()).createUser(any(), any());
    }

    @Test
    void skipsWhenAdminAlreadyExists() {

        when(userService.usernameExists("admin")).thenReturn(true);

        AdminSeeder seeder = new AdminSeeder(
                userService, "admin", "admin@example.com", "StrongPass123");

        seeder.run(null);

        verify(userService, never()).createUser(any(), any());
    }
}
