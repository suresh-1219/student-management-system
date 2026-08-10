package com.suresh.sms.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final Key key;


  
    // CONSTRUCTOR
    

    public JwtUtil(
            @Value("${jwt.secret}") String secret) {

        this.key = Keys.hmacShaKeyFor(
                secret.getBytes()
        );
    }


   
    // GENERATE TOKEN
    

    public String generateToken(
            String username,
            String role) {

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 3600000
                        )
                )
                .signWith(key)
                .compact();
    }


   
    // EXTRACT ROLE
    
    public String extractRole(String token) {

        Claims claims =
                Jwts.parser()
                        .verifyWith(
                                (javax.crypto.SecretKey) key
                        )
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

        return claims.get(
                "role",
                String.class
        );
    }


    
    // EXTRACT USERNAME
    
    public String extractUsername(String token) {

        Claims claims =
                Jwts.parser()
                        .verifyWith(
                                (javax.crypto.SecretKey) key
                        )
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

        return claims.getSubject();
    }


   
    // VALIDATE TOKEN
    
    public boolean validateToken(String token) {

        try {

            Jwts.parser()
                    .verifyWith(
                            (javax.crypto.SecretKey) key
                    )
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}