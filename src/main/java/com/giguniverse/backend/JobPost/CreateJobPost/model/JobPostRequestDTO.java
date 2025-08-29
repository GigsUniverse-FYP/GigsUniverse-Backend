package com.giguniverse.backend.JobPost.CreateJobPost.model;

import java.util.List;
import lombok.Data;

@Data
public class JobPostRequestDTO {
    private String jobTitle;
    private String jobDescription;
    private String jobScope;
    private Boolean isPremiumJob;
    private List<String> skillTags;
    private String jobField;
    private List<String> jobCategory;
    private String yearsOfExperienceFrom;
    private String yearsOfExperienceTo;
    private List<String> jobExperience;
    private String hoursContributionPerWeek;
    private List<String> highestEducationLevel;
    private String jobStatus;
    private String payRateFrom;
    private String payRateTo;
    private String durationValue;
    private String durationUnit;
    private String jobLocationHiring;
    private List<String> jobLocation;
    private String companyName;
    private List<String> languageProficiency;
}
