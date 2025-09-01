package com.giguniverse.backend.Feedback.Service;

import com.giguniverse.backend.Feedback.Model.FreelancerFeedback;
import com.giguniverse.backend.Feedback.Repository.FreelancerFeedbackRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class FreelancerFeedbackService {

    @Autowired
    private FreelancerFeedbackRepository feedbackRepository;

    public FreelancerFeedback saveFeedback(FreelancerFeedback dto) {
        FreelancerFeedback feedback = FreelancerFeedback.builder()
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
