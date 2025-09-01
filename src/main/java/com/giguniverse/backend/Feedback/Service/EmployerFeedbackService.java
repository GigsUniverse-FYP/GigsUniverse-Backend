package com.giguniverse.backend.Feedback.Service;

import com.giguniverse.backend.Feedback.Model.EmployerFeedback;
import com.giguniverse.backend.Feedback.Repository.EmployerFeedbackRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class EmployerFeedbackService {

    @Autowired
    private EmployerFeedbackRepository feedbackRepository;

    public EmployerFeedback saveEmployerFeedback(EmployerFeedback dto) {
        EmployerFeedback feedback = EmployerFeedback.builder()
                .rating(dto.getRating())
                .feedback(dto.getFeedback())
                .employerId(dto.getEmployerId())
                .freelancerId(dto.getFreelancerId())
                .jobId(dto.getJobId())
                .contractId(dto.getContractId())
                .build();

        return feedbackRepository.save(feedback);
    }
}
