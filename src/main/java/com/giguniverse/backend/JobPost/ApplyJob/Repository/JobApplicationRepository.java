package com.giguniverse.backend.JobPost.ApplyJob.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.JobPost.ApplyJob.Model.JobApplication;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Integer> {
    int countByJobId(String jobId);
    boolean existsByJobIdAndFreelancerId(String jobId, String freelancerId);
    List<JobApplication> findByJobId(String jobId);

    List<JobApplication> findByFreelancerId(String freelancerId);
}
