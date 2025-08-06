package com.giguniverse.backend.Profile.Model.Mongo_Freelancer;

import lombok.*;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "freelancer_education")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FreelancerEducation {
    @Id
    private String id; 

    private String userId;

    private List<EducationItem> educationExperiences;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EducationItem {
        private String institute;
        private String title;
        private String courseName;
        private String fromDate;
        private String toDate;
        private boolean currentStudying;
    }
}
