package com.suresh.sms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.suresh.sms.dto.LoginRequest;
import com.suresh.sms.dto.RegisterRequest;
import com.suresh.sms.dto.TokenPair;
import com.suresh.sms.dto.UserResponseDTO;
import com.suresh.sms.entity.Role;
import com.suresh.sms.entity.User;
import com.suresh.sms.jwt.JwtUtil;
import com.suresh.sms.repository.UserRepository;
import com.suresh.sms.exception.DuplicateUserException;
import com.suresh.sms.exception.InvalidCredentialsException;
import com.suresh.sms.exception.InvalidRefreshTokenException;
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;


    
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


    // USER LOGIN (access token + refresh token)

    public TokenPair loginWithRefreshToken(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user);

        return new TokenPair(accessToken, refreshToken);
    }


    // REFRESH (rotates the refresh token: the old one stops working)

    public TokenPair refresh(String rawRefreshToken) {

        User user = refreshTokenService.validateAndRevoke(rawRefreshToken);

        if (!userRepository.existsById(user.getId())) {
            // The user was deleted after the refresh token was issued.
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        String newRefreshToken = refreshTokenService.issue(user);

        return new TokenPair(accessToken, newRefreshToken);
    }


    // LOGOUT (revokes the refresh token; the access token still expires on its own)

    public void logout(String rawRefreshToken) {

        refreshTokenService.revoke(rawRefreshToken);
    }


  
    // FIND USER BY USERNAME
 
    public User findByUsername(String username) {

        return userRepository.findByUsername(username)
                .orElse(null);
    }
}