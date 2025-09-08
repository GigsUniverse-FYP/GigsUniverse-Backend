package com.giguniverse.backend.JobMatch.service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Feedback.Model.FreelancerFeedback;
import com.giguniverse.backend.Feedback.Repository.FreelancerFeedbackRepository;
import com.giguniverse.backend.JobMatch.model.JobMatchResponse;
import com.giguniverse.backend.JobPost.ContractHandling.Repository.ContractRepository;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;
import com.giguniverse.backend.JobPost.CreateJobPost.repository.JobPostRepository;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Model.FreelancerProfile;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerJobExperience;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;
import com.giguniverse.backend.Profile.Repository.Mongo_Freelancer.FreelancerJobExperienceRepository;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;

@Service
public class MatchService {

    @Value("${engine.url}")
    private String engineUrl;

    private String engineEndpoint = engineUrl + "/predict";

    @PostConstruct
    private void init() {
        this.engineEndpoint = engineUrl + "/predict";
    }

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepo;
    @Autowired
    private JobPostRepository jobPostRepo;
    @Autowired
    private FreelancerJobExperienceRepository jobExpRepo;
    @Autowired
    private ContractRepository contractRepo;
    @Autowired
    private FreelancerFeedbackRepository freelancerFeedbackRepo;
    @Autowired
    private EmployerProfileRepository employerProfileRepo;

    // A Sample Testing Endpoint
    public Map<String, Object> matchFreelancerToJob(String freelancerId, Integer jobPostId) {
        // Fetch data
        FreelancerProfile freelancer = freelancerProfileRepo
                .findByFreelancer_FreelancerUserId(freelancerId)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        JobPost job = jobPostRepo.findById(jobPostId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        List<FreelancerJobExperience> expList = jobExpRepo.findByUserId(freelancerId);
        FreelancerJobExperience exp = expList.isEmpty() ? null : expList.get(0);

        // Calculate freelancer total years of experience 
        int totalYears = 0;
        if (exp != null && exp.getJobExperiences() != null) {
            for (FreelancerJobExperience.JobExperienceItem item : exp.getJobExperiences()) {
                try {
                    LocalDate from = LocalDate.parse(item.getFromDate());
                    LocalDate to = (item.isCurrentJob() || item.getToDate() == null || item.getToDate().isEmpty())
                            ? LocalDate.now()
                            : LocalDate.parse(item.getToDate());
                    totalYears += Period.between(from, to).getYears();
                } catch (Exception e) {
                    // ignore bad dates
                }
            }
        }

        // arse job payrate (string "25-35" → average) 
        Double jobPayrate = 0.0;
        if (job.getPreferredPayrate() != null) {
            String pr = job.getPreferredPayrate();
            if (pr.contains("-")) {
                String[] parts = pr.split("-");
                try {
                    double low = Double.parseDouble(parts[0].trim());
                    double high = Double.parseDouble(parts[1].trim());
                    jobPayrate = (low + high) / 2.0;
                } catch (Exception e) {
                    jobPayrate = 0.0;
                }
            } else {
                try {
                    jobPayrate = Double.parseDouble(pr.trim());
                } catch (Exception e) {
                    jobPayrate = 0.0;
                }
            }
        }

        // Build job_features (raw, for FastAPI encoder) 
        Map<String, Object> jobFeatures = new LinkedHashMap<>();
        jobFeatures.put("job_skills", job.getSkillTags());
        jobFeatures.put("job_languages_required", job.getLanguageProficiency());
        jobFeatures.put("job_location", job.getJobLocation());
        jobFeatures.put("job_hours_per_week", job.getHoursContributionPerWeek());
        jobFeatures.put("education_level", job.getHighestEducationLevel());
        jobFeatures.put("job_field", job.getJobField());
        jobFeatures.put("job_category", job.getJobCategory());
        jobFeatures.put("preferred_payrate", jobPayrate);
        jobFeatures.put("job_experience", job.getJobExperience());

        // Build freelancer_features (raw, for FastAPI encoder)
        Map<String, Object> freelancerFeatures = new LinkedHashMap<>();
        freelancerFeatures.put("freelancer_skills", freelancer.getSkillTags());
        freelancerFeatures.put("freelancer_languages", extractLanguages(freelancer.getLanguageProficiency()));
        freelancerFeatures.put("freelancer_location", freelancer.getLocation());
        freelancerFeatures.put("freelancer_experience_years", totalYears);
        freelancerFeatures.put("freelancer_hours_per_week", freelancer.getHoursPerWeek());
        freelancerFeatures.put("freelancer_payrate",
                freelancer.getPreferredPayrate() != null ? freelancer.getPreferredPayrate().doubleValue() : 0.0);
        freelancerFeatures.put("openToWork", freelancer.getOpenToWork() ? 1 : 0);
        freelancerFeatures.put("premiumStatus", freelancer.getPremiumStatus() ? 1 : 0);
        freelancerFeatures.put("highestEducationLevel", freelancer.getHighestEducationLevel());

        // Call FastAPI
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "jobId", jobPostId,
                "freelancerId", freelancerId,
                "job_features", jobFeatures,
                "freelancer_features", freelancerFeatures
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                engineEndpoint, HttpMethod.POST, request, new ParameterizedTypeReference<>() {});

        Map<String, Object> body = response.getBody();
        Double score = 0.0;
        if (body != null && body.get("match_score") != null) {
            Object val = body.get("match_score");
            if (val instanceof Number) {
                score = ((Number) val).doubleValue();
            } else {
                try {
                    score = Double.parseDouble(val.toString());
                } catch (Exception e) {
                    score = 0.0;
                }
            }
        }

        // Return result 
        return Map.of(
                "freelancerId", freelancerId,
                "jobId", jobPostId,
                "match_score", score,
                "job_features", jobFeatures,
                "freelancer_features", freelancerFeatures
        );
    }

    private String extractLanguages(Object raw) {
        if (raw == null) return "";
        try {
            if (raw instanceof String) {
                // already stored as JSON string → parse manually
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = mapper.readValue((String) raw, List.class);
                List<String> langs = new ArrayList<>();
                for (Map<String, Object> item : list) {
                    if (item.get("language") != null) {
                        langs.add(item.get("language").toString());
                    }
                }
                return String.join(",", langs);
            }
            if (raw instanceof List) {
                // directly stored as list of maps
                List<?> list = (List<?>) raw;
                List<String> langs = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof Map) {
                        Object lang = ((Map<?, ?>) o).get("language");
                        if (lang != null) langs.add(lang.toString());
                    }
                }
                return String.join(",", langs);
            }
        } catch (Exception e) {
            // fallback
            System.out.println("Error parsing languages: " + e.getMessage());
        }
        return raw.toString();
    }


    // ---------------------- Freelancer Side ----------------------


    private Map<String, Object> buildPayload(JobPost job, FreelancerProfile freelancer, List<FreelancerJobExperience> expList) {
        // --- calc total years of experience ---
        int totalYears = 0;
        if (expList != null && !expList.isEmpty()) {
            for (FreelancerJobExperience.JobExperienceItem item : expList.get(0).getJobExperiences()) {
                try {
                    LocalDate from = LocalDate.parse(item.getFromDate());
                    LocalDate to = (item.isCurrentJob() || item.getToDate() == null || item.getToDate().isEmpty())
                            ? LocalDate.now()
                            : LocalDate.parse(item.getToDate());
                    totalYears += Period.between(from, to).getYears();
                } catch (Exception ignored) {}
            }
        }

        // --- parse job payrate ---
        Double jobPayrate = 0.0;
        if (job.getPreferredPayrate() != null) {
            String pr = job.getPreferredPayrate();
            if (pr.contains("-")) {
                try {
                    String[] parts = pr.split("-");
                    double low = Double.parseDouble(parts[0].trim());
                    double high = Double.parseDouble(parts[1].trim());
                    jobPayrate = (low + high) / 2.0;
                } catch (Exception ignored) {}
            } else {
                try {
                    jobPayrate = Double.parseDouble(pr.trim());
                } catch (Exception ignored) {}
            }
        }

        // --- job features ---
        Map<String, Object> jobFeatures = new LinkedHashMap<>();
        jobFeatures.put("job_skills", job.getSkillTags());
        jobFeatures.put("job_languages_required", job.getLanguageProficiency());
        jobFeatures.put("job_location", job.getJobLocation());
        jobFeatures.put("job_hours_per_week", job.getHoursContributionPerWeek());
        jobFeatures.put("education_level", job.getHighestEducationLevel());
        jobFeatures.put("job_field", job.getJobField());
        jobFeatures.put("job_category", job.getJobCategory());
        jobFeatures.put("preferred_payrate", jobPayrate);
        jobFeatures.put("job_experience", job.getJobExperience());

        // --- freelancer features ---
        Map<String, Object> freelancerFeatures = new LinkedHashMap<>();
        freelancerFeatures.put("freelancer_skills", freelancer.getSkillTags());
        freelancerFeatures.put("freelancer_languages", extractLanguages(freelancer.getLanguageProficiency()));
        freelancerFeatures.put("freelancer_location", freelancer.getLocation());
        freelancerFeatures.put("freelancer_experience_years", totalYears);
        freelancerFeatures.put("freelancer_hours_per_week", freelancer.getHoursPerWeek());
        freelancerFeatures.put("freelancer_payrate",
                freelancer.getPreferredPayrate() != null ? freelancer.getPreferredPayrate().doubleValue() : 0.0);
        freelancerFeatures.put("openToWork", freelancer.getOpenToWork() ? 1 : 0);
        freelancerFeatures.put("premiumStatus", freelancer.getPremiumStatus() ? 1 : 0);
        freelancerFeatures.put("highestEducationLevel", freelancer.getHighestEducationLevel());

        // --- full payload ---
        Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", job.getJobPostId());
        payload.put("freelancerId", freelancer.getFreelancer().getFreelancerUserId());
        payload.put("job_features", jobFeatures);
        payload.put("freelancer_features", freelancerFeatures);

        return payload;
    }

    public Map<String, Object> matchFreelancerToTopJob(String freelancerId, Integer jobPostId) {
        FreelancerProfile freelancer = freelancerProfileRepo
                .findByFreelancer_FreelancerUserId(freelancerId)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        JobPost job = jobPostRepo.findById(jobPostId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        List<FreelancerJobExperience> expList = jobExpRepo.findByUserId(freelancerId);

        Map<String, Object> requestBody = buildPayload(job, freelancer, expList);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                engineEndpoint, HttpMethod.POST, request, new ParameterizedTypeReference<>() {}
        );

        Double score = Optional.ofNullable(response.getBody())
                .map(b -> b.get("match_score"))
                .map(Object::toString)
                .map(Double::parseDouble)
                .orElse(0.0);

        return Map.of(
                "freelancerId", freelancerId,
                "jobId", jobPostId,
                "match_score", score,
                "job_features", requestBody.get("job_features"),
                "freelancer_features", requestBody.get("freelancer_features")
        );
    }

    public List<JobMatchResponse> getTopMatchesForCurrentUser() {
        String freelancerId = AuthUtil.getUserId();
        FreelancerProfile freelancer = freelancerProfileRepo
                .findByFreelancer_FreelancerUserId(freelancerId)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        List<FreelancerJobExperience> expList = jobExpRepo.findByUserId(freelancerId);
        List<JobPost> activeJobs = jobPostRepo.findByJobStatus("Active");

        List<JobMatchResponse> results = new ArrayList<>();

        for (JobPost job : activeJobs) {
            Map<String, Object> payload = buildPayload(job, freelancer, expList);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    engineEndpoint,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {}
            );

            Double score = Optional.ofNullable(response.getBody())
                    .map(b -> b.get("match_score"))
                    .map(Object::toString)
                    .map(Double::parseDouble)
                    .orElse(0.0);

            results.add(new JobMatchResponse(job.getJobPostId(), score, job));
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(JobMatchResponse::getMatchScore).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }


    // ---------------------- Employer Side ----------------------

    public List<Map<String, Object>> getTopTalentsForEmployer() {
        String employerId = AuthUtil.getUserId();

        List<JobPost> jobs = jobPostRepo.findByEmployerIdAndJobStatus(employerId, "Active");
        List<FreelancerProfile> freelancers = freelancerProfileRepo.findAll();

        List<Map<String, Object>> scoredTalents = new ArrayList<>();

        for (JobPost job : jobs) {
            for (FreelancerProfile freelancer : freelancers) {
                List<FreelancerJobExperience> expList = jobExpRepo.findByUserId(
                        freelancer.getFreelancer().getFreelancerUserId()
                );

                Map<String, Object> payload = buildPayload(job, freelancer, expList);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        engineEndpoint, HttpMethod.POST, request, new ParameterizedTypeReference<>() {}
                );

                Double matchScore = Optional.ofNullable(response.getBody())
                        .map(b -> b.get("match_score"))
                        .map(Object::toString)
                        .map(Double::parseDouble)
                        .orElse(0.0);

                // calculate freelancer rating
                Double avgRating = Optional.ofNullable(
                                freelancerFeedbackRepo.findByFreelancerId(freelancer.getFreelancer().getFreelancerUserId()))
                        .orElse(Collections.emptyList())
                        .stream()
                        .mapToInt(FreelancerFeedback::getRating)
                        .average()
                        .orElse(0.0);

                // count completed jobs
                Long completedJobs = Optional.ofNullable(
                                contractRepo.findByFreelancerIdAndContractStatus(
                                        freelancer.getFreelancer().getFreelancerUserId(), "completed"))
                        .orElse(Collections.emptyList())
                        .stream().count();

                String avatar = buildAvatar(freelancer);

                Map<String, Object> talentMap = new HashMap<>();
                talentMap.put("id", freelancer.getFreelancer().getFreelancerUserId());
                talentMap.put("name", freelancer.getFullName());
                talentMap.put("title", job.getJobTitle());
                talentMap.put("location", freelancer.getLocation());
                talentMap.put("hourlyRate", freelancer.getPreferredPayrate());
                talentMap.put("rating", avgRating);
                talentMap.put("completedJobs", completedJobs);
                talentMap.put("match", matchScore);
                talentMap.put("skills", parseSkills(freelancer.getSkillTags()));
                talentMap.put("avatar", avatar);
                talentMap.put("jobId", job.getJobPostId());
                scoredTalents.add(talentMap);
            }
        }

        // sort safely
        return scoredTalents.stream()
                .filter(t -> {
                    Double rating = parseDouble(t.get("rating"));
                    return rating >= 4.0;
                })
                .sorted((a, b) -> {
                    Double matchA = parseDouble(a.get("match"));
                    Double matchB = parseDouble(b.get("match"));
                    return matchB.compareTo(matchA);
                })
                .limit(15)
                .collect(Collectors.toList());
    }

    private Double parseDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // helper: convert skillTags string to List<String>
    private List<String> parseSkills(String skillTags) {
        if (skillTags == null || skillTags.isEmpty()) return Collections.emptyList();
        return Arrays.stream(skillTags.split(","))
                .map(String::trim)
                .toList();
    }

    // helper: build freelancer avatar
    private String buildAvatar(FreelancerProfile freelancer) {
        if (freelancer.getProfilePicture() == null || freelancer.getProfilePictureMimeType() == null) {
            return null;
        }
        String base64Image = Base64.getEncoder().encodeToString(freelancer.getProfilePicture());
        return "data:" + freelancer.getProfilePictureMimeType() + ";base64," + base64Image;
    }


    @Autowired
    private JavaMailSender mailSender;

    // --- send invitation ---
    public void sendInvitationEmail(String freelancerUserId, Integer jobPostId) {
        try {
            String employerId = AuthUtil.getUserId();

            EmployerProfile employer = employerProfileRepo
                    .findByEmployer_EmployerUserId(employerId)
                    .orElseThrow(() -> new RuntimeException("Employer not found"));

            String employerName = employer.getFullName();

            FreelancerProfile freelancer = freelancerProfileRepo
                    .findByFreelancer_FreelancerUserId(freelancerUserId)
                    .orElseThrow(() -> new RuntimeException("Freelancer not found"));

            String freelancerName = freelancer.getFullName();
            String freelancerEmail = freelancer.getEmail();

            JobPost job = jobPostRepo.findById(jobPostId)
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            String jobTitle = job.getJobTitle();

            String body = "<div style='font-family:Arial,sans-serif;font-size:14px;color:#333;'>"
                    + "<p>Dear " + freelancerName + ",</p>"
                    + "<p>You have been invited by <strong>" + employerName + "</strong> to apply for the following job:</p>"
                    + "<p><strong>Job:</strong> " + jobTitle + " (ID: " + jobPostId + ")</p>"
                    + "<p>Please login to your account to view the invitation and job information.</p>"
                    + "<br>"
                    + "<p><a href='https://gigsuniverse.studio/dashboard/freelancer/job-search?id=" + jobPostId + "' target='_blank'>View Job Invitation</a></p>"
                    + "<br>"
                    + "<p>Regards,<br>GigsUniverse Team</p>"
                    + "</div>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("admin@gigsuniverse.studio");
            helper.setTo(freelancerEmail);
            helper.setSubject("Job Invitation from " + employerName + " - GigsUniverse");
            helper.setText(body, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send invitation email");
        }
    }
}
