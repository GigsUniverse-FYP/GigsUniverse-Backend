package com.giguniverse.backend.Feedback.Repository;

import com.giguniverse.backend.Feedback.Model.FreelancerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FreelancerFeedbackRepository extends JpaRepository<FreelancerFeedback, Integer> {

    // Optional: get all feedbacks for a specific freelancer
    List<FreelancerFeedback> findByFreelancerId(String freelancerId);

    // Optional: get feedbacks for a specific contract
    List<FreelancerFeedback> findByContractId(int contractId);

    // Optional: get feedbacks for a specific job
    List<FreelancerFeedback> findByJobId(int jobId);
}
