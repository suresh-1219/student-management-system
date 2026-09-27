package com.suresh.sms.controller;

import com.suresh.sms.support.AbstractIntegrationTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.suresh.sms.dto.LoginRequest;
import com.suresh.sms.dto.LoginResponse;
import com.suresh.sms.dto.RefreshTokenRequest;
import com.suresh.sms.dto.TokenPair;
import com.suresh.sms.exception.InvalidRefreshTokenException;
import com.suresh.sms.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;


    
    // LOGIN SUCCESS TEST
   

    @Test
    void testLogin() throws Exception {

        LoginRequest request =
                new LoginRequest(
                        "suresh",
                        "password"
                );

        when(userService.loginWithRefreshToken(any(LoginRequest.class)))
                .thenReturn(new TokenPair("test-token", "test-refresh-token"));

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token")
                .value("test-token"))
        .andExpect(jsonPath("$.refreshToken")
                .value("test-refresh-token"));
    }


    // REFRESH

    @Test
    void testRefreshReturnsNewTokenPair() throws Exception {

        when(userService.refresh("old-refresh-token"))
                .thenReturn(new TokenPair("new-access-token", "new-refresh-token"));

        mockMvc.perform(
                post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest("old-refresh-token")))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("new-access-token"))
        .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void testRefreshRejectsInvalidToken() throws Exception {

        when(userService.refresh("bad-token"))
                .thenThrow(new InvalidRefreshTokenException("Invalid or expired refresh token"));

        mockMvc.perform(
                post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest("bad-token")))
        )
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void testRefreshRejectsBlankToken() throws Exception {

        mockMvc.perform(
                post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"  \"}")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }


    // LOGOUT

    @Test
    void testLogoutReturnsNoContent() throws Exception {

        mockMvc.perform(
                post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest("some-refresh-token")))
        )
        .andExpect(status().isNoContent());

        verify(userService).logout("some-refresh-token");
    }

    // LOGIN VALIDATION

    @Test
    void testLoginRejectsBlankUsername() throws Exception {

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("  ", "password")))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.username").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void testLoginRejectsMissingPassword() throws Exception {

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"suresh\"}")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(userService);
    }
}
