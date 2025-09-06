package com.giguniverse.backend.JobMatch.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;
import com.giguniverse.backend.JobPost.CreateJobPost.repository.JobPostRepository;
import com.giguniverse.backend.Profile.Model.FreelancerProfile;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerJobExperience;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;
import com.giguniverse.backend.Profile.Repository.Mongo_Freelancer.FreelancerJobExperienceRepository;

@Service
public class MatchService {

    // @Value("${engine.url}")
    private String engineUrl = "https://engine.gigsuniverse.studio/predict";

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepo;

    @Autowired
    private JobPostRepository jobPostRepo;

    @Autowired
    private FreelancerJobExperienceRepository jobExpRepo;

    public Map<String, Object> matchFreelancerToJob(String freelancerId, Integer jobPostId) {
        // === 1. Fetch data ===
        FreelancerProfile freelancer = freelancerProfileRepo
                .findByFreelancer_FreelancerUserId(freelancerId)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        JobPost job = jobPostRepo.findById(jobPostId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        List<FreelancerJobExperience> expList = jobExpRepo.findByUserId(freelancerId);
        FreelancerJobExperience exp = expList.isEmpty() ? null : expList.get(0);

        // === 2. Calculate experience years ===
        int totalYears = 0;
        if (exp != null && exp.getJobExperiences() != null) {
            for (FreelancerJobExperience.JobExperienceItem item : exp.getJobExperiences()) {
                LocalDate from = LocalDate.parse(item.getFromDate());
                LocalDate to = (item.isCurrentJob() || item.getToDate().isEmpty())
                        ? LocalDate.now()
                        : LocalDate.parse(item.getToDate());
                totalYears += Period.between(from, to).getYears();
            }
        }

        // === 3. Build features map ===
        Map<String, Object> features = new HashMap<>();
        features.put("years_experience", totalYears);
        features.put("education_level", freelancer.getHighestEducationLevel());

        // Encode job experience levels (Entry, Mid, etc.)
        if (job.getJobExperience() != null) {
            for (String expLevel : job.getJobExperience().split(",")) {
                features.put("job_req_exp_" + expLevel.trim().toLowerCase().replace(" ", "_"), 1);
            }
        }

        // Freelancer skills
        if (freelancer.getSkillTags() != null) {
            for (String skill : freelancer.getSkillTags().split(",")) {
                features.put("skill_" + skill.trim().toLowerCase(), 1);
            }
        }

        // Job required skills
        if (job.getSkillTags() != null) {
            for (String skill : job.getSkillTags().split(",")) {
                features.put("job_req_" + skill.trim().toLowerCase(), 1);
            }
        }

        // === 4. Call FastAPI ===
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Wrap features map inside another map with key "features"
    Map<String, Object> requestBody = Map.of("features", features);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            engineUrl, HttpMethod.POST, request, new ParameterizedTypeReference<>() {});

    Double score = null;
    Map<String, Object> body = response.getBody();
    if (body != null && body.containsKey("match_score")) {
        score = ((Number) body.get("match_score")).doubleValue();
    }

        // === 5. Return result ===
        return Map.of(
                "freelancerId", freelancerId,
                "jobId", jobPostId,
                "match_score", score,
                "features", features
        );
    }
}
