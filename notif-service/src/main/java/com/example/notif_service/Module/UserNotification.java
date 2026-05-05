package com.example.notif_service.Module;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "user_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;


    @Column(name = "user_id")  
    private int userId;

    @Column(name = "notif_id") 
    private int notifId;

    private boolean isRead = false;

    // ===== Relations =====

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notif_id", insertable = false, updatable = false)
    private Notification notification;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}