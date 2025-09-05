package com.giguniverse.backend.Dashboard.Users.Model;

import lombok.Data;

@Data
public class UserRecordDTO {
    private String userId;
    private String role;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private boolean onlineStatus;
    private Boolean premiumStatus; 
    private String profilePictureBase64; 
    private String profilePictureMimeType;
    private java.time.LocalDateTime accountCreationDate;
    private boolean accountBannedStatus;
    private String bannedReason;
    private java.time.LocalDateTime unbanDate;
}


