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

    public ChatSessionDTO(String id, LastMessageInfo lastMessage, Map<String, Integer> unreadCount,
            List<ChatUserInfoDTO> participants, Map<String, String> roles,
            boolean groupChat, String groupName, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
        this.participants = participants;
        this.roles = roles;
        this.groupChat = groupChat;
        this.groupName = groupName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
