package com.giguniverse.backend.Profile.Model.DTO;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreelancerProfileFormData {
    private String freelancerProfileId;
    private String fullName;
    private String username;
    private String gender;
    private String dob;
    private String phone;
    private String location;
    private String selfDescription;
    private String highestEducationLevel;
    private String profilePictureMimeType;
    private int hoursPerWeek;
    private List<String> jobCategory;
    private List<String> preferredJobTitle;
    private List<String> skillTags;
    private List<LanguageProficiency> languageProficiency;
    private String preferredPayRate;
    private boolean openToWork;
    private List<JobExperience> jobExperiences;
    private List<Education> educations;
    
    @Data
    public static class LanguageProficiency {
        private String language;
        private String proficiency;
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
