package com.suresh.sms.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

        // STUDENT NOT FOUND TEST
    
    @Test
    void testStudentNotFoundException() {

        StudentNotFoundException exception =
                new StudentNotFoundException("Student not found");

        ResponseEntity<Map<String, Object>> response =
                handler.handleStudentNotFound(exception);

        assertEquals(
                404,
                response.getStatusCode().value()
        );

        assertEquals(
                "Student not found",
                response.getBody().get("message")
        );
    }

    
    // VALIDATION EXCEPTION TEST
    

    @Test
    void testValidationException() {

        BindingResult bindingResult =
                mock(BindingResult.class);

        FieldError fieldError =
                new FieldError(
                        "student",
                        "name",
                        "Name is required"
                );

        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(fieldError));

        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);

        when(exception.getBindingResult())
                .thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> response =
                handler.handleValidationErrors(exception);

        assertEquals(
                400,
                response.getStatusCode().value()
        );

        assertEquals(
                "Validation failed",
                response.getBody().get("message")
        );

        @SuppressWarnings("unchecked")
        Map<String, String> errors =
                (Map<String, String>)
                        response.getBody().get("errors");

        assertTrue(
                errors.containsKey("name")
        );

        assertEquals(
                "Name is required",
                errors.get("name")
        );
    }

    
    // DUPLICATE USER TEST
  

    @Test
    void testDuplicateUserException() {

        DuplicateUserException exception =
                new DuplicateUserException(
                        "Username already exists"
                );

        ResponseEntity<Map<String, Object>> response =
                handler.handleDuplicateUserException(exception);

        assertEquals(
                409,
                response.getStatusCode().value()
        );

        assertEquals(
                "Username already exists",
                response.getBody().get("message")
        );
    }

    
    // RUNTIME EXCEPTION TEST
  

    @Test
    void testInvalidCredentialsException() {

        InvalidCredentialsException exception =
                new InvalidCredentialsException("Invalid username or password");

        ResponseEntity<Map<String, Object>> response =
                handler.handleInvalidCredentials(exception);

        assertEquals(
                401,
                response.getStatusCode().value()
        );

        assertEquals(
                "Invalid username or password",
                response.getBody().get("message")
        );
    }

    // INVALID REQUEST (bad sort field / page size) -> 400

    @Test
    void testInvalidRequestException() {

        ResponseEntity<Map<String, Object>> response =
                handler.handleInvalidRequest(
                        new InvalidRequestException("Invalid sort field"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid sort field", response.getBody().get("message"));
    }


    // MALFORMED JSON -> 400

    @Test
    void testUnreadableBody() {

        ResponseEntity<Map<String, Object>> response =
                handler.handleUnreadableBody(
                        new HttpMessageNotReadableException(
                                "JSON parse error: internal detail",
                                mock(HttpInputMessage.class)));

        assertEquals(400, response.getStatusCode().value());
        // internal parser detail must not be echoed to the client
        assertEquals(
                "Malformed or unreadable request body",
                response.getBody().get("message"));
    }


    // WRONG TYPE IN URL (e.g. /students/abc) -> 400

    @Test
    void testTypeMismatch() {

        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException(
                        "abc", Long.class, "id", null, null);

        ResponseEntity<Map<String, Object>> response =
                handler.handleTypeMismatch(ex);

        assertEquals(400, response.getStatusCode().value());
        assertEquals(
                "Invalid value for parameter 'id'",
                response.getBody().get("message"));
    }


    // DATABASE CONSTRAINT -> 409

    @Test
    void testDataIntegrityViolation() {

        ResponseEntity<Map<String, Object>> response =
                handler.handleDataIntegrity(
                        new DataIntegrityViolationException(
                                "Duplicate entry 'x' for key 'users.username'"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals(
                "Request conflicts with existing data",
                response.getBody().get("message"));
    }


    // CATCH-ALL: unknown errors -> 500 with a generic message

    @Test
    void testUnexpectedExceptionDoesNotLeakDetails() {

        ResponseEntity<Map<String, Object>> response =
                handler.handleUnexpected(
                        new RuntimeException("jdbc:mysql://secret-host password=hunter2"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals(
                "An unexpected error occurred",
                response.getBody().get("message"));
        assertTrue(
                !response.getBody().toString().contains("hunter2"));
    }


    // CATCH-ALL keeps the status of Spring MVC's own exceptions

    @Test
    void testSpringMvcMissingParameterKeeps400() {

        ResponseEntity<Map<String, Object>> response =
                handler.handleUnexpected(
                        new MissingServletRequestParameterException("page", "int"));

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testSpringMvcMethodNotSupportedKeeps405() {

        ResponseEntity<Map<String, Object>> response =
                handler.handleUnexpected(
                        new HttpRequestMethodNotSupportedException("PATCH"));

        assertEquals(405, response.getStatusCode().value());
    }


    // EVERY ERROR HAS THE SAME SHAPE

    @Test
    void testErrorBodyShape() {

        Map<String, Object> body =
                handler.handleStudentNotFound(
                        new StudentNotFoundException("missing")).getBody();

        assertEquals(404, body.get("status"));
        assertEquals("Not Found", body.get("error"));
        assertEquals("missing", body.get("message"));
        assertTrue(body.containsKey("timestamp"));
    }
}
