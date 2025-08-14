package com.giguniverse.backend.Profile.Model.DTO;

import java.util.List;

import lombok.Data;

@Data
public class EmployerProfileDataResponse {
    
    // PostgreSQL Profile Fields
    private String employerProfileId;
    private String fullName;
    private String username;
    private String gender;
    private String dob;
    private String email;
    private String phone;
    private String profilePicture;
    private String profilePictureMimeType; 
    private String location;
    private String selfDescription;
    private String languageProficiency;
    private Boolean openToHire;
    private Boolean premiumStatus;


    // MongoDB Profile Fields
    private List<JobExperience> jobExperiences;
    private List<Education> educations;
    private List<CertificateFile> certificationFiles;

    @Data
    public static class CertificateFile {
        private String fileName;
        private String base64Data;
        private String contentType;
    }
    
    @Data
    public static class JobExperience {
        private String jobTitle;
        private String fromDate;
        private String toDate;
        private String company;
        private String description;
        private boolean currentJob;
    }
    
    @Data
    public static class Education {
        private String institute;
        private String title;
        private String courseName;
        private String fromDate;
        private String toDate;
        private boolean currentStudying;
    }
}
