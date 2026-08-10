package com.suresh.sms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.suresh.sms.dto.LoginRequest;
import com.suresh.sms.dto.UserResponseDTO;
import com.suresh.sms.entity.User;
import com.suresh.sms.jwt.JwtUtil;
import com.suresh.sms.repository.UserRepository;
import com.suresh.sms.exception.DuplicateUserException;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;


    
    // USER REGISTRATION
    
    public UserResponseDTO register(User user) {

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateUserException("Username already exists");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateUserException("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);

        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    
    // USER LOGIN
    
    public String login(LoginRequest request) {

        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new RuntimeException("Invalid username or password")
                );

        // Check password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException("Invalid username or password");
        }

        // Generate JWT token
        return jwtUtil.generateToken(
                user.getUsername(),
                user.getRole()
        );
    }


  
    // FIND USER BY USERNAME
 
    public User findByUsername(String username) {

        return userRepository.findByUsername(username)
                .orElse(null);
    }
}