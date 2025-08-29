package com.giguniverse.backend.JobPost.ApplyJob.Repository;

import com.giguniverse.backend.JobPost.ApplyJob.Model.FavouriteJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavouriteJobRepository extends JpaRepository<FavouriteJob, Integer> {
    List<FavouriteJob> findByFreelancerId(String freelancerId);

        // Check if a favourite exists for a freelancer and job
    Optional<FavouriteJob> findByFreelancerIdAndJobId(String freelancerId, String jobId);

    // Delete a favourite for a freelancer and job
    void deleteByFreelancerIdAndJobId(String freelancerId, String jobId);
}
