package com.giguniverse.backend.Chat.Model;

import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Document("private_chat_sessions")
public class ChatSession {

    @Id private String chatID; // the ID of Chat Session
    private List<String> participants;
    private ChatMessage lastMessage; // The Last message in the Chat Session
    private Map<String, Integer> unreadCount; // Mapping UserID to unread Message Counts

    // Handle Group Chats
    private boolean isGroupChat; // true if group chat, false if private chat
    private String groupName; // Name of the group chat, if applicable
}


