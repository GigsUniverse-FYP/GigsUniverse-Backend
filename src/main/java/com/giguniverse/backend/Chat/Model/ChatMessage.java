package com.giguniverse.backend.Chat.Model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Document(collection = "chat_messages")
public class ChatMessage {
    @Id
    private String id;
    @Indexed
    private String chatId;
    private String senderId;
    private String receiverId;
    private String textContent;
    private List<String> fileName;
    private List<String> fileContent;
    private List<String> fileType;
    private List<Long> fileSizes;
    private Instant timestamp;
    private boolean read;
}
