package com.giguniverse.backend.JobPost.ApplyJob.Model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FreelancerApplicationDTO {
    private int id;
    private String jobTitle;
    private String company;
    private String appliedDate;
    private String status;
    private String rejectReason;
    private String jobExperience;
    private String level;
    private String salary;
    private String salaryOffered;
    private String location;
    private String type;
    private String proposal;
    private String jobPostId;
    private String employerId;
}
