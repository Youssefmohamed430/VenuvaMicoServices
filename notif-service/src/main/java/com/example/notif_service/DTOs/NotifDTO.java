package com.example.notif_service.DTOs;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Builder
public class NotifDTO {

    private int notifId;

    private String message;

    private LocalDateTime date = LocalDateTime.now();

    private int userId;

    private String userName;

    private boolean isRead = false;

    public NotifDTO(int notifId, String message, LocalDateTime date, int userId, String userName, boolean isRead) {
        this.notifId = notifId;
        this.message = message;
        this.date = date;
        this.userId = userId;
        this.userName = userName;
        this.isRead = isRead;
    }
    public int getNotifId() {
        return notifId;
    }
    public void setNotifId(int notifId) {
        this.notifId = notifId;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public LocalDateTime getDate() {
        return date;
    }
    public void setDate(LocalDateTime date) {
        this.date = date;
    }
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public boolean isRead() {
        return isRead;
    }
    public void setRead(boolean read) {
        isRead = read;
    }
    
}