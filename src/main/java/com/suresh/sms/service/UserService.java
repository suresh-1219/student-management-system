package com.suresh.sms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.suresh.sms.dto.LoginRequest;
import com.suresh.sms.dto.RegisterRequest;
import com.suresh.sms.dto.UserResponseDTO;
import com.suresh.sms.entity.Role;
import com.suresh.sms.entity.User;
import com.suresh.sms.jwt.JwtUtil;
import com.suresh.sms.repository.UserRepository;
import com.suresh.sms.exception.DuplicateUserException;
import com.suresh.sms.exception.InvalidCredentialsException;
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;


    
    // USER REGISTRATION (public endpoint - always creates a plain USER)

    public UserResponseDTO register(RegisterRequest request) {

        return createUser(request, Role.USER);
    }


    // INTERNAL USER CREATION
    // Not reachable from any controller. Used by public registration (USER)
    // and by the startup AdminSeeder (ADMIN).

    public UserResponseDTO createUser(RegisterRequest request, Role role) {

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUserException("Username already exists");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateUserException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);

        User savedUser = userRepository.save(user);

        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }


    public boolean usernameExists(String username) {

        return userRepository.existsByUsername(username);
    }


    // USER LOGIN
    
    public String login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return jwtUtil.generateToken(user.getUsername(), user.getRole().name());
    }


  
    // FIND USER BY USERNAME
 
    public User findByUsername(String username) {

        return userRepository.findByUsername(username)
                .orElse(null);
    }
}