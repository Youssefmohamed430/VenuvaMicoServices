package com.example.notif_service.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.example.AOP.Annotation.Loggable;
import com.example.notif_service.Abstractions.Error;
import com.example.notif_service.Abstractions.Result;
import com.example.notif_service.DTOs.CreateNotificationDto;
import com.example.notif_service.DTOs.NotifDTO;
import com.example.notif_service.Module.Notification;
import com.example.notif_service.Module.UserNotification;
import com.example.notif_service.Repos.NotificationRepository;
import com.example.notif_service.Repos.UserNotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotifService implements INotifService {

    private final UserNotificationRepository userNotifRepoGeneric;
    private final NotificationRepository notifRepo;

    @Override
    @Loggable(value = "GetNotificationsById", logArguments = true, logResult = false)
    public Result<List<NotifDTO>> getNotifsById(int id) {
        log.info("NotifService.getNotifsById() called with userId={}", id);

        List<UserNotification> notifs = userNotifRepoGeneric.findAll()
                .stream()
                .filter(n -> n.getUserId() == id)
                .collect(Collectors.toList());

        List<NotifDTO> dtoList = notifs.stream()
                .map(n -> {
                    Notification notif = notifRepo.findById(n.getNotifId()).orElse(null);
                    if (notif == null) return null;

                    return new NotifDTO(
                            n.getId(),
                            notif.getMessage(),
                            notif.getDate(),
                            n.getUserId(),
                            "Unknown",
                            n.isRead()
                    );
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        log.info("NotifService.getNotifsById() success: {} notifications retrieved for userId={}", dtoList.size(), id);
        return Result.success(dtoList);
    }

    @Override
    @Loggable(value = "MarkNotificationRead", logArguments = true, logResult = false)
    public Result<NotifDTO> markNotifAsRead(int notifId) {
        log.info("[START] NotifService.markNotifAsRead() — notifId={}", notifId);

        Optional<UserNotification> notifOptional = userNotifRepoGeneric.findById(notifId);

        if (!notifOptional.isPresent()) {
            log.warn("[WARN] NotifService.markNotifAsRead() — Notification not found: {}", notifId);
            return Result.failure(new Error("Notif.NotFound", "Notification not found"));
        }

        UserNotification notif = notifOptional.get();
        Notification n = notifRepo.findById(notif.getNotifId()).orElse(null);

        if (n == null) {
            log.warn("[WARN] NotifService.markNotifAsRead() — Notification not found: {}", notifId);
            return Result.failure(new Error("Notif.NotFound", "Notification not found"));
        }

        notif.setRead(true);
        userNotifRepoGeneric.save(notif);

        log.info("[OK] NotifService.markNotifAsRead() — Notification {} marked as read", notifId);
        return Result.success(new NotifDTO(
                                n.getNotifId(),
                                n.getMessage(),
                                n.getDate(),
                                notif.getUserId(),
                                "Unknown",
                                notif.isRead()
                        ));
    }

    @Override
    @Loggable(value = "SendNotification", logArguments = false, logResult = false)
    public Result<Object> sendNotification(String message) {
        log.info("[START] NotifService.sendNotification() — Broadcasting message");

        Notification notif = new Notification();
        notif.setMessage(message);
        notif.setDate(LocalDateTime.now());

        notifRepo.save(notif);

        log.info("[OK] NotifService.sendNotification() — Notification created with id={}", notif.getNotifId());
        return Result.success(null);
    }

    /**
     * Create a notification for a specific user.
     * Called by:
     *   - POST /api/notifications (REST endpoint)
     *   - RabbitMQ consumers (EventConsumer, PaymentConsumer, RegistrationConsumer)
     */
    @Override
    @Loggable(value = "CreateNotification", logArguments = false, logResult = false)
    public Result<NotifDTO> createNotification(CreateNotificationDto dto) {
        log.info("[START] NotifService.createNotification() — userId={}, eventId={}", dto.getUserId(), dto.getEventId());

        // Save global notification message
        Notification notif = new Notification();
        notif.setMessage(dto.getMessage());
        notif.setDate(LocalDateTime.now());
        notifRepo.save(notif);

        // Link notification to specific user
        UserNotification userNotif = UserNotification.builder()
                .userId(dto.getUserId())
                .notifId(notif.getNotifId())
                .isRead(false)
                .build();
        userNotifRepoGeneric.save(userNotif);

        log.info("[OK] NotifService.createNotification() — Created notifId={} for userId={}",
                notif.getNotifId(), dto.getUserId());

        return Result.success(new NotifDTO(
                notif.getNotifId(),
                notif.getMessage(),
                notif.getDate(),
                dto.getUserId(),
                "Unknown",
                false
        ));
    }
}