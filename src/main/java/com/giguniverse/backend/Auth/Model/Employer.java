package com.giguniverse.backend.Auth.Model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.giguniverse.backend.Profile.Model.EmployerProfile;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Entity
@Table(name = "employer")
public class Employer {

    // A Special ID for employers.
    @Id
    @Column(unique = true)
    private String employerUserId;

    // Login Credentials
    @Column(unique = true)
    private String email;
    private String password;

    // Decision to Dashboard
    private String role;

    // Check Latest Login Date
    private LocalDateTime lastLoginDate;

    // User uses custom registration or OAuth
    private String registrationProvider;

    // Confirmation of Email Registration
    private boolean emailConfirmed = false;

    // Token for Email Confirmation
    private String emailConfirmationToken;

    // Check Confirmation Email Sent Duration
    private LocalDateTime lastEmailSentAt;

    // Account registration date
    private LocalDateTime registrationDate;

    // check user online status
    private boolean onlineStatus = false;

    // for banning information
    private boolean accountBannedStatus = false;
    private String bannedReason;
    private LocalDateTime unbanDate;

    // Reset Password Token
    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiry;

    // Onboarding Information
    private boolean completedOnboarding= false;
    private boolean completedProfile = false;
    
    private boolean completedIdentity = false;
    @Column(unique = true)
    private String sumsubApplicantId;
    @Enumerated(EnumType.STRING)
    private SumsubStatus sumsubStatus;

    @OneToOne(mappedBy = "employer", cascade = CascadeType.ALL)
    private EmployerProfile profile;
}