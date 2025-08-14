package com.giguniverse.backend.Chat.Model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDTO {

    private String id;
    private List<ChatUserInfoDTO> participants; // user details instead of just IDs
    private LastMessageInfo lastMessage;
    private Map<String, Integer> unreadCount; // userId : unread count
    private Map<String, String> roles; // userId : role ("admin", "member" in group chats)
    private boolean groupChat; 
    private String groupName; // only present if groupChat == true
    private Instant createdAt;
    private Instant updatedAt;

    // frontend use
    private String currentUserId;            
    private String type;                    
    private String displayName;             
    private String otherUserId;              
    private String displayAvatar;            
    private String displayAvatarInitials;    
    private Integer unreadForCurrentUser;   
    private String otherUsername;
    private Boolean otherIsOnline;
}
