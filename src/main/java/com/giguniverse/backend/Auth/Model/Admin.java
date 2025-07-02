package com.giguniverse.backend.Auth.Model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import jakarta.persistence.*;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Entity
@Table(name = "admin")
public class Admin {

    // A Special ID for admins.
    @Id
    @Column(unique = true)
    private String adminUserId;

    // Login Credentials
    @Column(unique = true)
    private String email;
    private String password;

    // Decision to Dashboard
    private String role;

    // Check Latest Login Date
    private LocalDateTime lastLoginDate;

    // check user online status
    private boolean onlineStatus = false;

    // Reset Password Token
    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiry;
}
