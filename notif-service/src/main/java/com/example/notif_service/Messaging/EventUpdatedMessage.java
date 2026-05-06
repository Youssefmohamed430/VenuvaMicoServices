package com.example.notif_service.Messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventUpdatedMessage {
    private int eventId;
    private String title;
    private String date;
    private String location;
    private String message;
}
