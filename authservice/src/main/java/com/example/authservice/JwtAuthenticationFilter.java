package com.example.authservice;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.authservice.Models.UserDetails.User;
import com.example.authservice.Repos.UserRepository;
import com.example.authservice.Services.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();
        
        log.debug("[JwtAuthenticationFilter] {} {}", method, path);

        if (isPublicPath(path)) {
            log.debug("Skipping JWT filter for public path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Skipping JWT filter - No Bearer token found");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String email;

        try {
            email = jwtService.extractEmail(token);
            log.debug("Email extracted from token: {}", email);
        } catch (Exception e) {
            log.error("Failed to extract email from token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid token\"}");
            return;
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("Attempting to authenticate user: {}", email);

            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user != null) {
                log.debug("👤 User found: {}, Role: {}", user.getEmail(), user.getRole());
                
                if (jwtService.isTokenValid(token, email)) {
                    log.debug("Token is valid");
                    
                    List<String> roles = jwtService.extractRoles(token);
                    log.debug("Roles: {}", roles);
                    
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> {
                                String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                                log.debug("  └─ Authority: {}", roleWithPrefix);
                                return new SimpleGrantedAuthority(roleWithPrefix);
                            })
                            .toList();

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    authorities
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("User authenticated: {} with authorities: {}", email, authorities);
                } else {
                    log.warn("Token is invalid");
                }
            } else {
                log.warn("User not found in database");
            }
        }

        filterChain.doFilter(request, response);
    }

        private boolean isPublicPath(String path) {
            return path.equals("/api/auth/login") ||
                path.equals("/api/auth/register") ||
                path.equals("/api/auth/check-email") ||
                path.equals("/api/registrations/register") ||
                path.equals("/api/Paymob/callback") ||
                path.equals("/api/auth/refresh-token") ||
                path.equals("/error");
        }
    }