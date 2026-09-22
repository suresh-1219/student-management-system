package com.suresh.sms.controller;

import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suresh.sms.dto.StudentDTO;
import com.suresh.sms.service.StudentService;

/**
 * Checks request validation only. The service is mocked on purpose: this test
 * used to call the REAL service, which inserted a new "Suresh" row into the
 * real database on every run (and now fails with 409 because student e-mails
 * are unique).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class StudentValidationTest {

    @MockitoBean
    private StudentService studentService;

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
                        BigDecimal.valueOf(50000.0)
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
                        BigDecimal.valueOf(50000.0)
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
                        BigDecimal.valueOf(50000.0)
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
                        BigDecimal.valueOf(50000.0)
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
                        BigDecimal.valueOf(-5000.0)
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
                        BigDecimal.valueOf(50000.0)
                );

        when(studentService.saveStudent(any(StudentDTO.class)))
                .thenReturn(dto);

        mockMvc.perform(
                post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(dto)
                        )
        )
        .andExpect(status().isOk());

        verify(studentService).saveStudent(any(StudentDTO.class));
    }
}