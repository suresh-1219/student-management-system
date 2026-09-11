package com.suresh.sms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.suresh.sms.jwt.JwtFilter;

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

                // Everything else
                .anyRequest().authenticated()
            )

            
            // EXCEPTION HANDLING
           
            .exceptionHandling(exception -> exception

                // No authentication -> 401
                .authenticationEntryPoint(
                    new HttpStatusEntryPoint(
                        HttpStatus.UNAUTHORIZED
                    )
                )

                // Authenticated but wrong role -> 403
                .accessDeniedHandler(
                    (request, response, accessDeniedException) -> {
                        response.setStatus(
                            HttpStatus.FORBIDDEN.value()
                        );
                    }
                )
            );

       
        // JWT FILTER
        
        http.addFilterBefore(
            jwtFilter,
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }


   
    // PASSWORD ENCODER
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}