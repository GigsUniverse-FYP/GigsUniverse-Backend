package com.giguniverse.backend.Chat.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatUserDTO {
    private String userId;
    private String fullName;
    private String username;
    private String role;
    private String profileImageBase64;
    private String profileMimeType;
}

