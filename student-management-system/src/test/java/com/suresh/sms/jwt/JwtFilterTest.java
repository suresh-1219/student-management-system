package com.suresh.sms.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.mock.web.MockFilterChain;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtFilter jwtFilter;


    @BeforeEach
    void setUp() {

        SecurityContextHolder.clearContext();
    }


   
    // VALID TOKEN TEST
    

    @Test
    void testValidToken() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();


        request.addHeader(
                "Authorization",
                "Bearer test-token"
        );


        when(jwtUtil.validateToken("test-token"))
                .thenReturn(true);

        when(jwtUtil.extractUsername("test-token"))
                .thenReturn("suresh");

        when(jwtUtil.extractRole("test-token"))
                .thenReturn("USER");


        jwtFilter.doFilter(
                request,
                response,
                filterChain
        );


        assertNotNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        assertEquals(
                "suresh",
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal()
        );

        assertEquals(
                "ROLE_USER",
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority()
        );
    }


   
    // NO TOKEN TEST
    

    @Test
    void testNoToken() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();


        jwtFilter.doFilter(
                request,
                response,
                filterChain
        );


        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );
    }


   
    // INVALID TOKEN TEST
   

    @Test
    void testInvalidToken() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();


        request.addHeader(
                "Authorization",
                "Bearer invalid-token"
        );


        when(jwtUtil.validateToken("invalid-token"))
                .thenReturn(false);


        jwtFilter.doFilter(
                request,
                response,
                filterChain
        );


        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );
    }
}