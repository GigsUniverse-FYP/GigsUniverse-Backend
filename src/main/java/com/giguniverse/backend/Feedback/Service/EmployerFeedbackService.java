package com.giguniverse.backend.Feedback.Service;

import com.giguniverse.backend.Feedback.Model.EmployerFeedback;
import com.giguniverse.backend.Feedback.Repository.EmployerFeedbackRepository;

import java.util.List;

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

    public EmployerRatingResponse getEmployerRating(String employerId) {
        List<EmployerFeedback> feedbackList = feedbackRepository.findByEmployerId(employerId);

        int totalRatings = feedbackList.size();
        double averageRating = 0.0;

        if (totalRatings > 0) {
            int sum = feedbackList.stream()
                    .mapToInt(EmployerFeedback::getRating)
                    .sum();
            averageRating = (double) sum / totalRatings;
            averageRating = Math.round(averageRating * 10.0) / 10.0; 
        }

        return new EmployerRatingResponse(averageRating, totalRatings);
    }

    public static record EmployerRatingResponse(double averageRating, int totalRatings) {}
}
