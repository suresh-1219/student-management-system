package com.suresh.sms.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.suresh.sms.dto.UserResponseDTO;
import com.suresh.sms.dto.RegisterRequest;
import com.suresh.sms.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;


    
    // REGISTER USER TEST
   

    @Test
    void testRegister() throws Exception {

        // Request
        RegisterRequest user = new RegisterRequest(
                "suresh", "suresh@gmail.com", "password123");


        // Service Response
        UserResponseDTO savedUser =
                new UserResponseDTO();

        savedUser.setId(1L);
        savedUser.setUsername("suresh");
        savedUser.setEmail("suresh@gmail.com");
        savedUser.setRole("USER");


        // Mock service
        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(savedUser);


        // Call API
        mockMvc.perform(
                post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(user)
                        )
        )

        // Verify response
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.username").value("suresh"))
        .andExpect(jsonPath("$.email")
                .value("suresh@gmail.com"))
        .andExpect(jsonPath("$.role").value("USER"));
    }

    // SECURITY: a client-supplied role must be ignored

    @Test
    void testRegisterIgnoresRoleSentByClient() throws Exception {

        UserResponseDTO savedUser =
                new UserResponseDTO(1L, "mallory", "mallory@gmail.com", "USER");

        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(savedUser);

        String maliciousBody = """
                {
                  "username": "mallory",
                  "email": "mallory@gmail.com",
                  "password": "password123",
                  "role": "ADMIN"
                }
                """;

        mockMvc.perform(
                post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousBody)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("USER"));

        // The service only ever receives username/email/password.
        ArgumentCaptor<RegisterRequest> captor =
                ArgumentCaptor.forClass(RegisterRequest.class);

        verify(userService).register(captor.capture());

        assertEquals("mallory", captor.getValue().username());
    }


    // VALIDATION

    @Test
    void testRegisterRejectsShortPassword() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "suresh", "suresh@gmail.com", "short");

        mockMvc.perform(
                post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void testRegisterRejectsPasswordLongerThanBcryptLimit() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "suresh", "suresh@gmail.com", "a".repeat(73));

        mockMvc.perform(
                post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void testRegisterRejectsInvalidEmail() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "suresh", "not-an-email", "password123");

        mockMvc.perform(
                post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.email").exists());
    }
}
