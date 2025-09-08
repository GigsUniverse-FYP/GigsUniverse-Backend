package com.giguniverse.backend.JobMatch.model;

import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchResponse {
    private Integer jobId;
    private Double matchScore;
    private JobPost jobPost;
    private List<String> skills; // new field

    public JobMatchResponse(Integer jobId, Double matchScore, JobPost jobPost) {
        this.jobId = jobId;
        this.matchScore = matchScore;
        this.jobPost = jobPost;

        // parse skillTags into a list
        if (jobPost.getSkillTags() != null && !jobPost.getSkillTags().isBlank()) {
            this.skills = Arrays.stream(jobPost.getSkillTags().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } else {
            this.skills = Collections.emptyList();
        }
    }
}
