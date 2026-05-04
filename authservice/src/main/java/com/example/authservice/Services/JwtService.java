package com.example.authservice.Services;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import com.example.authservice.Repos.UserRepository;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Data
@Service
@ConfigurationProperties(prefix = "app.jwt")

public class JwtService {
    
    private String secret;
    private long expirationMs;
    private final UserRepository userRepository;
    private long refreshExpirationMs;



    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email, String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return Jwts.builder()
                .setSubject(email)
                .claim("role",  roleWithPrefix)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public List<String> extractRoles(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return List.of(claims.get("role", String.class));
    }


    public boolean isTokenValid(String token, String email) {
        String extractedEmail = extractEmail(token);
        return extractedEmail != null && extractedEmail.equals(email) && !isTokenExpired(token);
    }
    private boolean isTokenExpired(String token) {
        Date expirationDate = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        return expirationDate.before(new Date());
    }
}