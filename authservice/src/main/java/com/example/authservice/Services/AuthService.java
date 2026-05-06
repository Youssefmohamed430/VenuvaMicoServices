package com.example.authservice.Services;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.AOP.Annotation.Loggable;
import com.example.authservice.AuthDtos.AuthResponse;
import com.example.authservice.AuthDtos.LoginRequest;
import com.example.authservice.AuthDtos.RegisterRequest;
import com.example.authservice.AuthDtos.UserResponseDto;
import com.example.authservice.Exceptions.DataConflictException;
import com.example.authservice.Models.UserDetails.RefreshToken;
import com.example.authservice.Models.UserDetails.Roles;
import com.example.authservice.Models.UserDetails.User;
import com.example.authservice.Repos.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final RefreshTokenService refreshTokenService;


    // ===== Check Email =====
        public boolean checkEmail(String email) {
                return userRepository.findByEmail(email).isPresent();
                }

    // ===== Get Current User =====
        public AuthResponse getCurrentUser(String email) {
                log.info("[START] AuthService.getCurrentUser() — email={}", email);

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Email not found"));

                String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

                log.info("[OK] AuthService.getCurrentUser() — Retrieved user {}", email);
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

                return new AuthResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        token,
                        refreshToken.getToken(),
                        user.getUsername()
                );
        }
        @Loggable(value = "Login", logArguments = false, logResult = false)
        public AuthResponse login(LoginRequest loginDto) {
                log.info("[START] AuthService.login() — email={}", loginDto.getEmail());

                User user = userRepository.findByEmail(loginDto.getEmail())
                        .orElseThrow(() -> {
                                log.warn("[WARN] AuthService.login() — Email not found: {}", loginDto.getEmail());
                                return new RuntimeException("Email not found");
                        });

                if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
                        log.warn("[WARN] AuthService.login() — Wrong password for email={}", loginDto.getEmail());
                        throw new RuntimeException("Unauthorized");
                }

                String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

                log.info("[OK] AuthService.login() — User {} logged in successfully", loginDto.getEmail());
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

                return new AuthResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        token,
                        refreshToken.getToken(),
                        user.getUsername()
                );
        }

        @Loggable(value = "Register Organizer", logArguments = false, logResult = false)
        public AuthResponse registerOrganizer(RegisterRequest dto) {
                log.info("[START] AuthService.registerOrganizer() — username='{}', email={}, role=ORGANIZER", 
                        dto.getUsername(), dto.getEmail());

                // Check for duplicate email BEFORE saving
                Optional<User> existingUser = userRepository.findByEmail(dto.getEmail());
                if (existingUser.isPresent()) {
                        log.warn("[WARN] AuthService.registerOrganizer() — Email already in use: {}", dto.getEmail());
                        throw new DataConflictException("This email address is already registered. Please use a different email.");
                }

                User user = User.builder()
                        .email(dto.getEmail())
                        .password(passwordEncoder.encode(dto.getPassword()))
                        .username(dto.getUsername())
                        .role(Roles.ORGANIZER)
                        .enabled(true)
                        .build();

                userRepository.save(user);

                String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

                log.info("[OK] AuthService.registerOrganizer() — Organizer {} registered with role=ORGANIZER", dto.getEmail());
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

                return new AuthResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        token,
                        refreshToken.getToken(),
                        user.getUsername()
                );
        }
        
        @Loggable(value = "Register", logArguments = false, logResult = false)
        public AuthResponse register(RegisterRequest dto) {
                log.info("[START] AuthService.register() — username='{}', email={}, role=ATTENDEE", 
                        dto.getUsername(), dto.getEmail());

                // Check for duplicate email BEFORE saving
                Optional<User> existingUser = userRepository.findByEmail(dto.getEmail());
                if (existingUser.isPresent()) {
                        log.warn("[WARN] AuthService.register() — Email already in use: {}", dto.getEmail());
                        throw new DataConflictException("This email address is already registered. Please use a different email.");
                }

                User user = User.builder()
                        .username(dto.getUsername())
                        .email(dto.getEmail())
                        .password(passwordEncoder.encode(dto.getPassword()))
                        .role(Roles.ATTENDEE)
                        .enabled(true)
                        .build();
                
                userRepository.save(user);

                String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

                log.info("[OK] AuthService.register() — User {} registered with role=ATTENDEE", dto.getEmail());
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

                return new AuthResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        token,
                        refreshToken.getToken(),
                        user.getUsername()
                );
        }

        @Loggable(value = "Refresh Token", logArguments = false, logResult = false)
        public AuthResponse refreshToken(String requestToken) {
                RefreshToken refreshToken = refreshTokenService.findByToken(requestToken)
                        .orElseThrow(() -> new RuntimeException("Refresh token not found"));

                refreshTokenService.verifyExpiration(refreshToken); // throws if expired

                User user = refreshToken.getUser();
                String newAccessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());

                // Rotate: issue a new refresh token and invalidate the old one
                RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

                log.info("[OK] AuthService.refreshToken() — Token refreshed for user {}", user.getEmail());
                return new AuthResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        newAccessToken,
                        newRefreshToken.getToken(),
                        user.getUsername()
                );
                }

        // ===== Get User By ID (Internal — used by other microservices) =====
        @Loggable(value = "GetUserById", logArguments = true, logResult = false)
        public UserResponseDto getUserById(int id) {
                log.info("[START] AuthService.getUserById() — id={}", id);
                User user = userRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
                log.info("[OK] AuthService.getUserById() — Found user {}", user.getEmail());
                return new UserResponseDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole().name()
                );
        }

        // ===== Get Users By Role (Internal — used by other microservices) =====
        @Loggable(value = "GetUsersByRole", logArguments = true, logResult = false)
        public List<UserResponseDto> getUsersByRole(Roles role) {
                log.info("[START] AuthService.getUsersByRole() — role={}", role);
                List<User> users = userRepository.findByRole(role);
                log.info("[OK] AuthService.getUsersByRole() — Found {} users with role {}", users.size(), role);
                return users.stream().map(user -> new UserResponseDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole().name()
                )).collect(Collectors.toList());
        }
}