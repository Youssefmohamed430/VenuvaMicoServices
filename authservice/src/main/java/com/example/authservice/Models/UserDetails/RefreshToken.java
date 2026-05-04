// src/main/java/com/example/venuva/Core/Domain/Models/UserDetails/RefreshToken.java
package com.example.authservice.Models.UserDetails;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}