package com.example.notif_service.Repos;

import com.example.notif_service.Module.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

}
