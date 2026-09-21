package com.suresh.sms.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.suresh.sms.service.StudentService;

/**
 * Sends real HTTP-style requests through Spring MVC (security filters off) to
 * prove that bad input produces a clean 4xx JSON error instead of a 500.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ErrorHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;


    @Test
    void malformedJsonReturns400() throws Exception {

        mockMvc.perform(
                post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void nonNumericIdReturns400() throws Exception {

        mockMvc.perform(get("/students/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void missingRequestParameterReturns400() throws Exception {

        mockMvc.perform(get("/students/page"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidSortFieldReturns400() throws Exception {

        when(studentService.sortStudents("bogus"))
                .thenThrow(new InvalidRequestException("Invalid sort field"));

        mockMvc.perform(get("/students/sort/bogus"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid sort field"));
    }

    @Test
    void unsupportedMethodReturns405NotSomething500() throws Exception {

        mockMvc.perform(patch("/students/1"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unknownUrlReturns404() throws Exception {

        mockMvc.perform(get("/no-such-endpoint"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {

        when(studentService.getAllStudents())
                .thenThrow(new RuntimeException("secret-internal-detail"));

        mockMvc.perform(get("/students"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
        .andExpect(content().string(not(containsString("secret-internal-detail"))));
    }
}
