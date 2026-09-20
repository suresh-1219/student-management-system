package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.suresh.sms.dto.LoginRequest;
import com.suresh.sms.dto.RegisterRequest;
import com.suresh.sms.dto.UserResponseDTO;
import com.suresh.sms.entity.Role;
import com.suresh.sms.entity.User;
import com.suresh.sms.exception.DuplicateUserException;
import com.suresh.sms.jwt.JwtUtil;
import com.suresh.sms.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;


    
    // REGISTER TESTS

    @Test
    void testRegister() {

        RegisterRequest request = new RegisterRequest(
                "suresh", "suresh@gmail.com", "password123");

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        when(repository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO result = userService.register(request);

        assertNotNull(result);

        assertEquals("suresh", result.getUsername());
        assertEquals("suresh@gmail.com", result.getEmail());
        assertEquals("USER", result.getRole());
    }

    @Test
    void testRegisterAlwaysStoresUserRoleAndEncodedPassword() {

        RegisterRequest request = new RegisterRequest(
                "suresh", "suresh@gmail.com", "password123");

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        when(repository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());

        // Public registration can never produce an ADMIN
        assertEquals(Role.USER, captor.getValue().getRole());
        // Raw password must never reach the database
        assertEquals("encodedPassword", captor.getValue().getPassword());
    }

    @Test
    void testCreateUserWithAdminRole() {

        RegisterRequest request = new RegisterRequest(
                "admin", "admin@example.com", "password123");

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        when(repository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO result = userService.createUser(request, Role.ADMIN);

        assertEquals("ADMIN", result.getRole());
    }

    @Test
    void testRegisterDuplicateUsername() {

        RegisterRequest request = new RegisterRequest(
                "suresh", "suresh@gmail.com", "password123");

        when(repository.existsByUsername("suresh")).thenReturn(true);

        DuplicateUserException ex = assertThrows(
                DuplicateUserException.class,
                () -> userService.register(request));

        assertEquals("Username already exists", ex.getMessage());
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmail() {

        RegisterRequest request = new RegisterRequest(
                "suresh", "suresh@gmail.com", "password123");

        when(repository.existsByEmail("suresh@gmail.com")).thenReturn(true);

        DuplicateUserException ex = assertThrows(
                DuplicateUserException.class,
                () -> userService.register(request));

        assertEquals("Email already exists", ex.getMessage());
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void testUsernameExists() {

        when(repository.existsByUsername("suresh")).thenReturn(true);

        assertEquals(true, userService.usernameExists("suresh"));
        assertEquals(false, userService.usernameExists("nobody"));
    }


 
    // LOGIN SUCCESS TEST
  
    @Test
    void testLoginSuccess() {

        LoginRequest request =
                new LoginRequest("suresh", "password");

        User user = new User();

        user.setUsername("suresh");
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);

        when(repository.findByUsername("suresh"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password",
                "encodedPassword"))
                .thenReturn(true);

        when(jwtUtil.generateToken(
                "suresh",
                "USER"))
                .thenReturn("test-token");

        String result = userService.login(request);

        assertNotNull(result);

        assertEquals("test-token", result);
    }


   
    // INVALID USERNAME TEST
   
    @Test
    void testLoginInvalidUsername() {

        LoginRequest request =
                new LoginRequest("wrong", "password");

        when(repository.findByUsername("wrong"))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> userService.login(request)
                );

        assertEquals(
                "Invalid username or password",
                exception.getMessage()
        );
    }


   
    // INVALID PASSWORD TEST

    @Test
    void testLoginInvalidPassword() {

        LoginRequest request =
                new LoginRequest("suresh", "wrongPassword");

        User user = new User();

        user.setUsername("suresh");
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);

        when(repository.findByUsername("suresh"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrongPassword",
                "encodedPassword"))
                .thenReturn(false);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> userService.login(request)
                );

        assertEquals(
                "Invalid username or password",
                exception.getMessage()
        );
    }
}