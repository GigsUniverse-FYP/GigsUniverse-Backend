package com.giguniverse.backend.Profile.Model;

import java.time.LocalDate;

import com.giguniverse.backend.Auth.Model.Employer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Entity
@Table(name = "employer_profile")
public class EmployerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int employerProfileId;

    private String fullName;
    private String username;
    private String gender; 
    
    private LocalDate dob; 
    private String email;
    private String phone;
    private String location; 

    @Column(columnDefinition = "TEXT")
    private String languageProficiency;

    @Column(name = "profile_picture", columnDefinition = "bytea")
    private byte[] profilePicture;

    @Size(max = 10000, message = "Self-description must be under 10,000 characters.")
    private String selfDescription;

    private String currentPosition;

    private Boolean openToHire;
    private Boolean premiumStatus;

    private Integer availableCredits;

    @OneToOne
    @JoinColumn(name = "employer_user_id", referencedColumnName = "employerUserId")
    private Employer employer;
}
