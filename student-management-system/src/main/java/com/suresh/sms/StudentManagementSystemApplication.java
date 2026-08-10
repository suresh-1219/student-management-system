package com.suresh.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
    info = @Info(
        title = "Student Management System API",
        version = "1.0",
        description = "Spring Boot CRUD REST APIs"
    )
)
@SpringBootApplication
public class StudentManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementSystemApplication.class, args);
    }

}