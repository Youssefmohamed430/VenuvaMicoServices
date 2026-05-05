package com.example.notif_service.Messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Received when Event Service publishes a new event creation */
@Data @NoArgsConstructor @AllArgsConstructor
public class EventCreatedMessage {
    private int eventId;
    private String title;
    private String date;
    private String location;
    private String message;
}
