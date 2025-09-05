package com.giguniverse.backend.Feedback.Model;

import lombok.Data;

@Data
public class FreelancerFeedbackDTO {

    private int rating;
    private String feedback;
    private String employerId;
    private String freelancerId;
    private int jobId;
    private int contractId;
}