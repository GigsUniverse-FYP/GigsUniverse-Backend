package com.giguniverse.backend.Chat.Model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LastMessageInfo {
    private String messageId;
    private String senderId;
    private String contentPreview; // Short text or "[Image]"
    private Instant timestamp;
}
