package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.suresh.sms.dto.LoginRequest;
import com.suresh.sms.dto.UserResponseDTO;
import com.suresh.sms.entity.User;
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


    
    // REGISTER TEST
    
    @Test
    void testRegister() {

        User user = new User();

        user.setUsername("suresh");
        user.setEmail("suresh@gmail.com");
        user.setPassword("password");
        user.setRole("USER");

        when(passwordEncoder.encode("password"))
                .thenReturn("encodedPassword");

        when(repository.save(user))
                .thenReturn(user);

        UserResponseDTO result = userService.register(user);

        assertNotNull(result);

        assertEquals("suresh", result.getUsername());
        assertEquals("suresh@gmail.com", result.getEmail());
        assertEquals("USER", result.getRole());
    }


 
    // LOGIN SUCCESS TEST
  
    @Test
    void testLoginSuccess() {

        LoginRequest request =
                new LoginRequest("suresh", "password");

        User user = new User();

        user.setUsername("suresh");
        user.setPassword("encodedPassword");
        user.setRole("USER");

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
        user.setRole("USER");

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