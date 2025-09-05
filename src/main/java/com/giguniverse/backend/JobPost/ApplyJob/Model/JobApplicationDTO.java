package com.giguniverse.backend.JobPost.ApplyJob.Model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobApplicationDTO {
    private int id;
    private String freelancerId;
    private String freelancerName;
    private String freelancerAvatar; 
    private Integer hourlyRate;
    private String proposal;
    private String appliedAt;
    private String jobStatus;
    private double rating;
    private long completedJobs;
}

