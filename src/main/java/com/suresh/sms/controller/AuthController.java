package com.suresh.sms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.suresh.sms.dto.LoginRequest;
import com.suresh.sms.dto.LoginResponse;
import com.suresh.sms.dto.RefreshTokenRequest;
import com.suresh.sms.dto.TokenPair;
import com.suresh.sms.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {

        TokenPair tokens = userService.loginWithRefreshToken(request);

        return new LoginResponse(tokens.accessToken(), tokens.refreshToken());
    }

    /**
     * Exchanges a still-valid refresh token for a new access token. The
     * refresh token itself is rotated: the one supplied here stops working,
     * and a new one is returned alongside the new access token.
     */
    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {

        TokenPair tokens = userService.refresh(request.refreshToken());

        return new LoginResponse(tokens.accessToken(), tokens.refreshToken());
    }

    /**
     * Revokes a refresh token. The corresponding access token is not
     * invalidated - it simply expires on its own, typically within minutes.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {

        userService.logout(request.refreshToken());

        return ResponseEntity.noContent().build();
    }
}
