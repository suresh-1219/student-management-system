package com.suresh.sms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.suresh.sms.dto.RegisterRequest;
import com.suresh.sms.dto.UserResponseDTO;
import com.suresh.sms.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public UserResponseDTO register(@Valid @RequestBody RegisterRequest request) {

        return userService.register(request);
    }
}