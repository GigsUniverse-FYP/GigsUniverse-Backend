package com.giguniverse.backend.Profile.Model.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FreelancerFeedbackDTO {
    private String freelancerId;
    private String freelancerName;
    private int rating;
    private String feedback;
    private int hourlyRate;
    private String contractStartDate;
    private String contractEndDate;
    private String jobId;
    private String jobTitle;
}
