package com.giguniverse.backend.Profile.Model.Mongo_Freelancer;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "freelancer_job_experience")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreelancerJobExperience {

    @Id
    private String id; 

    private String userId;

    private List<JobExperienceItem> jobExperiences;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobExperienceItem {
        private String jobTitle;
        private String fromDate;
        private String toDate;
        private String company;
        private String description;
        private boolean currentJob;
    }
}
