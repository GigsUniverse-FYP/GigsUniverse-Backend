package com.giguniverse.backend.Feedback.Repository;

import com.giguniverse.backend.Feedback.Model.EmployerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployerFeedbackRepository extends JpaRepository<EmployerFeedback, Integer> {

    // Optional: get all feedbacks for a specific Employer
    List<EmployerFeedback> findByEmployerId(String EmployerId);

    // Optional: get feedbacks for a specific contract
    List<EmployerFeedback> findByContractId(int contractId);

    // Optional: get feedbacks for a specific job
    List<EmployerFeedback> findByJobId(int jobId);
}

