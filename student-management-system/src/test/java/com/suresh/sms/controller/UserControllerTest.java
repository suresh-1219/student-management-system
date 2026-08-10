package com.suresh.sms.controller;

import static org.mockito.ArgumentMatchers.any;
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

import com.suresh.sms.dto.UserResponseDTO;
import com.suresh.sms.entity.User;
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

        // Request User
        User user = new User();

        user.setUsername("suresh");
        user.setEmail("suresh@gmail.com");
        user.setPassword("password");
        user.setRole("USER");


        // Service Response
        UserResponseDTO savedUser =
                new UserResponseDTO();

        savedUser.setId(1L);
        savedUser.setUsername("suresh");
        savedUser.setEmail("suresh@gmail.com");
        savedUser.setRole("USER");


        // Mock service
        when(userService.register(any(User.class)))
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
}