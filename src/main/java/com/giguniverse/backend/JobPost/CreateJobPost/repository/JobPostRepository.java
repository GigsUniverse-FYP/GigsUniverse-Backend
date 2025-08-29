package com.giguniverse.backend.JobPost.CreateJobPost.repository;

import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Integer> {

    List<JobPost> findByEmployerIdOrderByCreatedAtDesc(String employerId);

    List<JobPost> findByJobPostId(Integer jobPostId);

    Optional<JobPost> findById(int id);

    List<JobPost> findByEmployerId(String employerId);

    List<JobPost> findByJobStatusNot(String status);

    List<JobPost> findByJobStatusAndJobExpirationDateBefore(String status, Date currentDate);

    List<JobPost> findByJobStatusNot(String status, Sort sort);

    Optional<JobPost> findByJobPostId(int jobPostId);

    @Query("SELECT COUNT(j) FROM JobPost j WHERE j.employerId = :employerId AND j.createdAt BETWEEN :start AND :end")
    int countByEmployerIdAndCreatedAtBetween(
            @Param("employerId") String employerId,
            @Param("start") Date start,
            @Param("end") Date end
    );
}
