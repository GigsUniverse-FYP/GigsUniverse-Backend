package com.giguniverse.backend.Feedback.Service;

import com.giguniverse.backend.Feedback.Model.FreelancerFeedback;
import com.giguniverse.backend.Feedback.Repository.FreelancerFeedbackRepository;

import java.util.List;

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


    public FreelancerRatingResponse getFreelancerRating(String freelancerId) {
        List<FreelancerFeedback> feedbackList = feedbackRepository.findByFreelancerId(freelancerId);

        int totalRatings = feedbackList.size();
        double averageRating = 0.0;

        if (totalRatings > 0) {
            int sum = feedbackList.stream()
                    .mapToInt(FreelancerFeedback::getRating)
                    .sum();
            averageRating = (double) sum / totalRatings;
            averageRating = Math.round(averageRating * 10.0) / 10.0; // round to 1 decimal place
        }

        return new FreelancerRatingResponse(averageRating, totalRatings);
    }

    public static record FreelancerRatingResponse(double averageRating, int totalRatings) {}
}
