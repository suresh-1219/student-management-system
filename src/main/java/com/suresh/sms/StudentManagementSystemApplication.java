package com.suresh.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Authentication is JWT-only (see SecurityConfig / JwtFilter), so Spring Boot's
// default in-memory user and its "Using generated security password" log line
// are not needed.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class StudentManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementSystemApplication.class, args);
    }

}
