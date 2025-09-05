package com.giguniverse.backend.Dashboard.Users.Model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UnsuspendUserRequest {
    private String userId;           
    private String role;            
    private String reason;
    private LocalDateTime endDate;  
}

