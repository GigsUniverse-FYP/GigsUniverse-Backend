package com.giguniverse.backend.Profile.Model.DTO;


import lombok.Data;

@Data
public class AdminProfileDataResponse {
    
    // PostgreSQL Profile Fields
    private String adminProfileId;
    private String fullName;
    private String username;
    private String gender;
    private String dob;
    private String email;
    private String phone;
    private String profilePicture;
    private String profilePictureMimeType; 
    private String location;
    private String languageProficiency;

}
