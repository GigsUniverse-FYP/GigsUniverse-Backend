package com.giguniverse.backend.Profile.Model;

import java.time.LocalDate;

import com.giguniverse.backend.Auth.Model.Freelancer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Entity
@Table(name = "freelancer_profile")
public class FreelancerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int freelancerProfileId;

    private String fullName;
    private String username;
    private String gender; // male / female
    
    private LocalDate dob; 
    private String email;
    private String phone;
    private String location;

    @Column(name = "profile_picture", columnDefinition = "bytea")
    private byte[] profilePicture;
    private String profilePictureMimeType;

    @Size(max = 10000, message = "Self-description must be under 10,000 characters.")
    @Column(columnDefinition = "TEXT")
    private String selfDescription;

    private String highestEducationLevel;

    private Integer hoursPerWeek;
    
    @Column(columnDefinition = "TEXT")
    private String jobCategory;

    @Column(columnDefinition = "TEXT")
    private String preferredJobTitle;

    @Column(columnDefinition = "TEXT")
    private String skillTags;

    @Column(columnDefinition = "TEXT")
    private String languageProficiency;


    private Integer preferredPayrate;

    private Boolean openToWork;
    
    private Boolean premiumStatus;

    @OneToOne
    @JoinColumn(name = "freelancer_user_id", referencedColumnName = "freelancerUserId")
    private Freelancer freelancer;
}


