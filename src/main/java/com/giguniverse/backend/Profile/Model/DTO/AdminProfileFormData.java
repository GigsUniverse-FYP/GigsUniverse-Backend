package com.giguniverse.backend.Profile.Model.DTO;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminProfileFormData {
    private String adminProfileId;
    private String fullName;
    private String username;
    private String gender;
    private String dob;
    private String phone;
    private String location;
    private String profilePictureMimeType;
    
    private List<LanguageProficiency> languageProficiency;

    @Data
    public static class LanguageProficiency {
        private String language;
        private String proficiency;
    }
}
