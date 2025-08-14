package com.giguniverse.backend.Chat.Model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Document(collection = "chat_sessions")
public class ChatSession {

    @Id
    private String id;

    @Indexed
    private List<String> participants; // list of participants (including the authenticated user + any other participants)

    private LastMessageInfo lastMessage; // show last message available in the chat session
    
    private Map<String, Integer> unreadCount; // userId : unread count

    private Map<String, String> roles; // userId : role (admin, member)

    private boolean groupChat; // is it a group chat or not

    private String groupName; // group name to display

    private Instant createdAt; // date created

    private Instant updatedAt; // date updated
}
