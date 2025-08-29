package com.giguniverse.backend.JobPost.CreateJobPost.model;
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
@Table(name = "job_post")
public class JobPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int jobPostId;

    private String jobTitle;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(columnDefinition = "TEXT")
    private String jobScope;

    private Boolean isPremiumJob;

    @Column(columnDefinition = "TEXT")
    private String skillTags; // skill required

    private String jobCategory; // part time, gig work or freelance

    private String jobField; // it, marketing etc.

    private String yearsOfJobExperience; // [5-10]

    private String jobExperience; // [entry level, mid level, senior level]

    private Integer hoursContributionPerWeek; // how many hours contribution required per week

    private String highestEducationLevel; // preferred education level store as [bachelor degree, associate degree]

    // active (make job available) / inactive (down the job) / expired (over 30 days) / full (max applications received)
    private String jobStatus; 

    private String preferredPayrate; // payrate per hour [ex. 25-30]

    @Column(columnDefinition = "TEXT")
    private String languageProficiency; // language proficiency required

    private String duration; // [ex. 3 months, 6 months]

    // to hire according to certain location only
    private Boolean jobLocationHiringRequired;
    private String jobLocation;

    // creation date
    private Date createdAt;

    // updated date
    private Date updatedAt;

    private Date jobExpirationDate;

    private String companyName; // personal / company name

    private int maxApplicationNumber; // store maximum application can be received

    private String employerId; // which employer created this job

    @Transient
    private String employerName;

    @Transient
    private boolean saved;

    @Transient
    private Long applicationsCount;
}
