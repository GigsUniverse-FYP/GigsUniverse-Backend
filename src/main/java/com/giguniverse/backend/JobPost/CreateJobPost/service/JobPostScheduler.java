package com.giguniverse.backend.JobPost.CreateJobPost.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;
import com.giguniverse.backend.JobPost.CreateJobPost.repository.JobPostRepository;

import java.util.*;

@Service
public class JobPostScheduler {

    private final JobPostRepository jobPostRepository;

    public JobPostScheduler(JobPostRepository jobPostRepository) {
        this.jobPostRepository = jobPostRepository;
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void expireJobs() {
        Date now = new Date();
        List<JobPost> jobs = jobPostRepository.findByJobStatusAndJobExpirationDateBefore("Active", now);

        for (JobPost job : jobs) {
            job.setJobStatus("Expired");
            job.setUpdatedAt(now);
        }

        if (!jobs.isEmpty()) {
            jobPostRepository.saveAll(jobs);
            System.out.println("Expired " + jobs.size() + " jobs at " + now);
        }
    }
}
