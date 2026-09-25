package com.suresh.sms.config;

import java.io.IOException;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.suresh.sms.jwt.JwtFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
           
            // CSRF
         
            .csrf(csrf -> csrf.disable())

          
            // STATELESS SESSION
           
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

          
            // DISABLE DEFAULT LOGIN
     
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

         
            // AUTHORIZATION
           
            .authorizeHttpRequests(auth -> auth

                // Public login
                .requestMatchers(
                    HttpMethod.POST,
                    "/auth/login"
                ).permitAll()

                // Public registration
                .requestMatchers(
                    HttpMethod.POST,
                    "/users/register"
                ).permitAll()

                // Swagger
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // Health/info checks (Docker, load balancers, uptime monitors).
                // No env/beans/etc are exposed (see application.properties), so
                // this is safe to leave public.
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info"
                ).permitAll()

                // USER + ADMIN can READ
                .requestMatchers(
                    HttpMethod.GET,
                    "/students/**"
                ).hasAnyRole("USER", "ADMIN")

                // ADMIN only - CREATE
                .requestMatchers(
                    HttpMethod.POST,
                    "/students/**"
                ).hasRole("ADMIN")

                // ADMIN only - UPDATE
                .requestMatchers(
                    HttpMethod.PUT,
                    "/students/**"
                ).hasRole("ADMIN")

                // ADMIN only - DELETE
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/students/**"
                ).hasRole("ADMIN")

                // USER + ADMIN can READ courses and enrollments
                .requestMatchers(
                    HttpMethod.GET,
                    "/courses/**",
                    "/enrollments/**"
                ).hasAnyRole("USER", "ADMIN")

                // ADMIN only - manage courses and enrollments (create, update, delete/unenroll)
                .requestMatchers(
                    HttpMethod.POST,
                    "/courses/**",
                    "/enrollments/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/courses/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/courses/**",
                    "/enrollments/**"
                ).hasRole("ADMIN")

                // Everything else
                .anyRequest().authenticated()
            )

            
            // EXCEPTION HANDLING

            .exceptionHandling(exception -> exception

                // No / invalid token -> 401 (JSON body)
                .authenticationEntryPoint(
                    (request, response, authException) ->
                        writeError(
                            response,
                            HttpStatus.UNAUTHORIZED,
                            "Authentication required: missing or invalid token"
                        )
                )

                // Authenticated but wrong role -> 403 (JSON body)
                .accessDeniedHandler(
                    (request, response, accessDeniedException) ->
                        writeError(
                            response,
                            HttpStatus.FORBIDDEN,
                            "You do not have permission to perform this action"
                        )
                )
            );

       
        // JWT FILTER
        
        http.addFilterBefore(
            jwtFilter,
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }


   
    // JSON ERROR BODY (same shape as GlobalExceptionHandler)
    // Messages are fixed strings, so no escaping of user input is needed.

    private static void writeError(
            HttpServletResponse response,
            HttpStatus status,
            String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message));
    }


    // PASSWORD ENCODER
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}