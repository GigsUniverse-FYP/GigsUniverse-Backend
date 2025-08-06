package com.giguniverse.backend.Profile.Model.Mongo_Employer;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "employer_education")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployerEducation {
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
