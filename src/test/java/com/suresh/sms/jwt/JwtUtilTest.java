package com.suresh.sms.jwt;

import com.suresh.sms.support.AbstractIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JwtUtilTest extends AbstractIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;


    
    // GENERATE TOKEN TEST
    
    @Test
    void testGenerateToken() {

        String token =
                jwtUtil.generateToken(
                        "suresh3",
                        "USER"
                );

        assertTrue(token != null);
        assertTrue(!token.isEmpty());
    }


    
    // EXTRACT USERNAME TEST
    

    @Test
    void testExtractUsername() {

        String token =
                jwtUtil.generateToken(
                        "suresh3",
                        "USER"
                );

        String username =
                jwtUtil.extractUsername(token);

        assertEquals(
                "suresh3",
                username
        );
    }


    
    // EXTRACT ROLE TEST
   

    @Test
    void testExtractRole() {

        String token =
                jwtUtil.generateToken(
                        "suresh3",
                        "USER"
                );

        String role =
                jwtUtil.extractRole(token);

        assertEquals(
                "USER",
                role
        );
    }


    
    // VALIDATE TOKEN TEST
    

    @Test
    void testValidateToken() {

        String token =
                jwtUtil.generateToken(
                        "suresh3",
                        "USER"
                );

        boolean valid =
                jwtUtil.validateToken(token);

        assertTrue(valid);
    }


    
    // INVALID TOKEN TEST
  

    @Test
    void testInvalidToken() {

        boolean valid =
                jwtUtil.validateToken(
                        "invalid.jwt.token"
                );

        assertTrue(!valid);
    }
}