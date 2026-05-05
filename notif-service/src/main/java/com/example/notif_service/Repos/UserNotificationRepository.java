package com.example.notif_service.Repos;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.notif_service.Module.UserNotification;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {
    
}
