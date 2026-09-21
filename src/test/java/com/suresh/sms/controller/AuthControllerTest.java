package com.suresh.sms.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.suresh.sms.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

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

        LoginResponse response =
                new LoginResponse("test-token");


        when(userService.login(any(LoginRequest.class)))
                .thenReturn("test-token");


        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token")
                .value("test-token"));
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
