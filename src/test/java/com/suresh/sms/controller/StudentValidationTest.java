package com.suresh.sms.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suresh.sms.dto.StudentDTO;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class StudentValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


   
    // EMPTY NAME
    

    @Test
    void testEmptyName() throws Exception {

        StudentDTO dto =
                new StudentDTO(
                        null,
                        "",
                        "test@gmail.com",
                        "MCA",
                        50000.0
                );

        mockMvc.perform(
                post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(dto)
                        )
        )
        .andExpect(status().isBadRequest());
    }


    
    // INVALID EMAIL
    
    @Test
    void testInvalidEmail() throws Exception {

        StudentDTO dto =
                new StudentDTO(
                        null,
                        "Suresh",
                        "wrong-email",
                        "MCA",
                        50000.0
                );

        mockMvc.perform(
                post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(dto)
                        )
        )
        .andExpect(status().isBadRequest());
    }


    
    // EMPTY EMAIL
   

    @Test
    void testEmptyEmail() throws Exception {

        StudentDTO dto =
                new StudentDTO(
                        null,
                        "Suresh",
                        "",
                        "MCA",
                        50000.0
                );

        mockMvc.perform(
                post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(dto)
                        )
        )
        .andExpect(status().isBadRequest());
    }


    // EMPTY COURSE
   
    @Test
    void testEmptyCourse() throws Exception {

        StudentDTO dto =
                new StudentDTO(
                        null,
                        "Suresh",
                        "suresh@gmail.com",
                        "",
                        50000.0
                );

        mockMvc.perform(
                post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(dto)
                        )
        )
        .andExpect(status().isBadRequest());
    }


   
    // NEGATIVE FEE
    

    @Test
    void testNegativeFee() throws Exception {

        StudentDTO dto =
                new StudentDTO(
                        null,
                        "Suresh",
                        "suresh@gmail.com",
                        "MCA",
                        -5000.0
                );

        mockMvc.perform(
                post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(dto)
                        )
        )
        .andExpect(status().isBadRequest());
    }


    
    // NULL FEE
   

    @Test
    void testNullFee() throws Exception {

        StudentDTO dto =
                new StudentDTO(
                        null,
                        "Suresh",
                        "suresh@gmail.com",
                        "MCA",
                        null
                );

        mockMvc.perform(
                post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(dto)
                        )
        )
        .andExpect(status().isBadRequest());
    }


    
    // VALID STUDENT
   

    @Test
    void testValidStudent() throws Exception {

        StudentDTO dto =
                new StudentDTO(
                        null,
                        "Suresh",
                        "suresh@gmail.com",
                        "MCA",
                        50000.0
                );

        mockMvc.perform(
                post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(dto)
                        )
        )
        .andExpect(status().isOk());
    }
}