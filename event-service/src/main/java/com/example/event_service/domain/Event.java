package com.example.event_service.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime date;
    private String location;
    private int maxAttendance;

    @Enumerated(EnumType.STRING)
    private EventStatus eventStatus;

    private boolean paymentRequired;
    private BigDecimal price;

    @Column(name = "organizer_id", nullable = false)
    private int organizerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
