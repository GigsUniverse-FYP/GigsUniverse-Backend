package com.giguniverse.backend.JobPost.ApplyJob.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.util.Date;

import jakarta.persistence.*;


@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
@Entity
@Table(name = "job_application")
public class JobApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int jobApplicationId;

    private String hourlyRate; // rate offered by the freelancer

    @Column(columnDefinition = "TEXT")
    private String jobProposal; // proposal or cover letter

    private Date appliedDate; // date of apply

    private String applicationStatus; // pending, shortlisted, rejected, contract

    @Column(columnDefinition = "TEXT")
    private String rejectInfo; // reason for rejection

    private String jobId; // id for the job

    private String freelancerId; // id of applied freelancer
}

