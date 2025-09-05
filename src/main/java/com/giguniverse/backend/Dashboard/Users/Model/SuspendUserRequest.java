package com.giguniverse.backend.Dashboard.Users.Model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SuspendUserRequest {
    private String userId;           
    private String role;            
    private String reason;
    private LocalDateTime endDate;  
}
