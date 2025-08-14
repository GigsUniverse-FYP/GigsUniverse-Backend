package com.giguniverse.backend.Profile.Model;

import java.time.LocalDate;

import com.giguniverse.backend.Auth.Model.Admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Entity
@Table(name = "admin_profile")
public class AdminProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int adminProfileId; 
        
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
    private String profilePictureMimeType;

    @OneToOne
    @JoinColumn(name = "admin_user_id", referencedColumnName = "adminUserId")
    private Admin admin;
}
