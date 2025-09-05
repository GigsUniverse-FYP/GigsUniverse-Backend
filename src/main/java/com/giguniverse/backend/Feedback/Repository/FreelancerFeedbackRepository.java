package com.giguniverse.backend.Feedback.Repository;

import com.giguniverse.backend.Feedback.Model.FreelancerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FreelancerFeedbackRepository extends JpaRepository<FreelancerFeedback, Integer> {

    List<FreelancerFeedback> findByFreelancerId(String freelancerId);

    List<FreelancerFeedback> findByContractId(int contractId);

    List<FreelancerFeedback> findByJobId(int jobId);

    List<FreelancerFeedback> findByFreelancerIdAndContractId(String freelancerId, int contractId);

 
}
