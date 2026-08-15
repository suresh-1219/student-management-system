package com.suresh.sms.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;


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
}