package com.suresh.sms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.suresh.sms.dto.LoginRequest;
import com.suresh.sms.dto.LoginResponse;
import com.suresh.sms.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {

        String token = userService.login(request);

        return new LoginResponse(token);
    }
}