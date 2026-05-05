package com.example.authservice.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.AOP.Annotation.HandleException;
import com.example.AOP.Annotation.Loggable;
import com.example.authservice.AuthDtos.AuthResponse;
import com.example.authservice.AuthDtos.LoginRequest;
import com.example.authservice.AuthDtos.RefreshTokenRequest;
import com.example.authservice.AuthDtos.RegisterRequest;
import com.example.authservice.AuthDtos.UserResponseDto;
import com.example.authservice.Models.UserDetails.User;
import com.example.authservice.Services.AuthService;
import com.example.authservice.Services.RefreshTokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;


    @PostMapping("/login")
    @HandleException
    @Loggable(value = "Login", logArguments = false, logResult = false)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("[PUBLIC] AuthController.login() — Action: User login attempt");
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @HandleException
    @Loggable(value = "Register", logArguments = false, logResult = false)
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("[PUBLIC] AuthController.register() — Action: New user registration");
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/organizer")
    @PreAuthorize("hasRole('ADMIN')")
    @HandleException
    @Loggable(value = "RegisterOrganizer", logArguments = false, logResult = false)
    public ResponseEntity<AuthResponse> registerOrganizer(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[ADMIN] AuthController.registerOrganizer() — User: {}", userDetails.getUsername());
        AuthResponse response = authService.registerOrganizer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @HandleException
    @Loggable(value = "GetCurrentUser", logArguments = false, logResult = false)
    public ResponseEntity<AuthResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("[USER] AuthController.getCurrentUser() — User: {}", userDetails.getUsername());
        AuthResponse response = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-email")
    @HandleException
    @Loggable(value = "CheckEmail", logArguments = true, logResult = false)
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        log.info("[PUBLIC] AuthController.checkEmail() — Checking email existence");
        boolean exists = authService.checkEmail(email);
        return ResponseEntity.ok(exists);
    }


    @PostMapping("/refresh-token")
    @HandleException
    @Loggable(value = "RefreshToken", logArguments = false, logResult = false)
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("[PUBLIC] AuthController.refreshToken() — Refreshing access token");
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @HandleException
    @Loggable(value = "Logout", logArguments = false, logResult = false)
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("[USER] AuthController.logout() — User: {}", userDetails.getUsername());
        // Cast to your User entity to get the ID
        User user = (User) userDetails;
        refreshTokenService.deleteByUserId(user.getUserId());
        return ResponseEntity.noContent().build();
    }

    // ===== INTERNAL ENDPOINTS (used by other microservices) =====

    /**
     * GET /api/auth/users/{id}
     * Used by: Registration Service (sync), Notification Service
     * Returns basic user info (id, name, email, role)
     */
    @GetMapping("/users/{id}")
    @HandleException
    @Loggable(value = "GetUserById", logArguments = true, logResult = false)
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable int id) {
        log.info("[INTERNAL] AuthController.getUserById() — id={}", id);
        UserResponseDto user = authService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * GET /api/auth/organizers/{id}
     * Used by: Event Service (sync) to display organizer name on events
     * Alias of getUserById — same data, semantic endpoint for clarity
     */
    @GetMapping("/organizers/{id}")
    @HandleException
    @Loggable(value = "GetOrganizerById", logArguments = true, logResult = false)
    public ResponseEntity<UserResponseDto> getOrganizerById(@PathVariable int id) {
        log.info("[INTERNAL] AuthController.getOrganizerById() — id={}", id);
        UserResponseDto organizer = authService.getUserById(id);
        return ResponseEntity.ok(organizer);
    }
}
