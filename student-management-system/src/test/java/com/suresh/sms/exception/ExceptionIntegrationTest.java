package com.suresh.sms.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.suresh.sms.entity.User;
import com.suresh.sms.repository.StudentRepository;
import com.suresh.sms.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ExceptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudentRepository studentRepository;

    @MockBean
    private UserService userService;


    
    // STUDENT NOT FOUND → 404
    

    @Test
    void testStudentNotFound() throws Exception {

        when(studentRepository.findById(999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                get("/students/999")
        )
        .andExpect(status().isNotFound())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Student not found with id : 999"
                        )
        );
    }


 
    // DUPLICATE USERNAME → 409
    

    @Test
    void testDuplicateUsername() throws Exception {

        User user = new User();

        user.setUsername("sureshduplicate");
        user.setEmail("unique@gmail.com");
        user.setPassword("password");
        user.setRole("USER");


        when(userService.register(any(User.class)))
                .thenThrow(
                        new DuplicateUserException(
                                "Username already exists"
                        )
                );


        mockMvc.perform(
                post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(user)
                        )
        )
        .andExpect(status().isConflict())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Username already exists"
                        )
        );
    }


  
    // DUPLICATE EMAIL → 409
    

    @Test
    void testDuplicateEmail() throws Exception {

        User user = new User();

        user.setUsername("uniqueusername");
        user.setEmail("duplicate@gmail.com");
        user.setPassword("password");
        user.setRole("USER");


        when(userService.register(any(User.class)))
                .thenThrow(
                        new DuplicateUserException(
                                "Email already exists"
                        )
                );


        mockMvc.perform(
                post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(user)
                        )
        )
        .andExpect(status().isConflict())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Email already exists"
                        )
        );
    }
}