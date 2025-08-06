package com.giguniverse.backend.Chat.Model;

import java.time.Instant;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Document("chat_messages")
public class ChatMessage {
    @Id
    private String id;
    private String chatId;
    private String fromUser;
    private String toUser;
    private String content;
    private String type; // e.g., TEXT, ATTACHMENT, EMOJI
    private Instant timestamp;
    private boolean read;

    // getters and setters
}
