package com.example.authservice.Services;

import com.example.authservice.Models.UserDetails.RefreshToken;
import com.example.authservice.Models.UserDetails.User;
import com.example.authservice.Repos.RefreshTokenRepository;
import com.example.authservice.Repos.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Transactional
    public RefreshToken createRefreshToken(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete any existing refresh token for this user (one token per user)
        refreshTokenRepository.deleteByUserId(userId);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            log.warn("Refresh token expired for user: {}", token.getUser().getEmail());
            throw new RuntimeException("Refresh token expired. Please log in again.");
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(int userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}