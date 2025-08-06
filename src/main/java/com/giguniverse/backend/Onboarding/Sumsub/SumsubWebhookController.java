package com.giguniverse.backend.Onboarding.Sumsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Model.SumsubStatus;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sumsub")
public class SumsubWebhookController {

    @Autowired
    private FreelancerRepository freelancerRepository;

    @Autowired
    private EmployerRepository employerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleSumsubWebhook(@RequestBody String payload) {
        try {
            Map<String, Object> json = objectMapper.readValue(payload, Map.class);

            System.out.println("📨 Webhook payload: " + payload);

            String applicantId = (String) json.get("applicantId");
            String externalUserId = (String) json.get("externalUserId");

            // Determine review result
            String reviewResult = null;
            if (json.containsKey("reviewResult")) {
                Map<String, Object> reviewResultMap = (Map<String, Object>) json.get("reviewResult");
                reviewResult = (String) reviewResultMap.get("reviewAnswer");
            }

            // Check for duplicate 
            boolean isDuplicate = false;
            if (json.containsKey("similarSearchInfo")) {
                Map<String, Object> similarSearchInfo = (Map<String, Object>) json.get("similarSearchInfo");
                String duplicateAnswer = (String) similarSearchInfo.get("answer");
                if ("RED".equalsIgnoreCase(duplicateAnswer)) {
                    isDuplicate = true;
                }
            }


            Object user = freelancerRepository.findById(externalUserId).orElse(null);
            if (user == null) {
                user = employerRepository.findById(externalUserId).orElse(null);
            }

            if (user == null) {
                return ResponseEntity.status(404).body("User not found for ID: " + externalUserId);
            }

            String userEmail = null;
            if (user instanceof Freelancer freelancer) {
                userEmail = freelancer.getEmail();
            } else if (user instanceof Employer employer) {
                userEmail = employer.getEmail();
            }

            if (userEmail == null) {
                return ResponseEntity.status(400).body("User email not found");
            }

            ResponseEntity<String> response = updateVerification(user, applicantId, reviewResult, isDuplicate);

            Map<String, Object> wsPayload = new HashMap<>();
                wsPayload.put("status", reviewResult != null ? reviewResult : "UNKNOWN");
                wsPayload.put("isDuplicate", isDuplicate);

                System.out.println("Sending to user: " + userEmail);
                messagingTemplate.convertAndSendToUser(
                    userEmail,
                    "/queue/sumsub-status",
                    wsPayload
            );

       
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("Invalid webhook payload");
        }
    }

    private ResponseEntity<String> updateVerification(Object user, String applicantId, String reviewResult, boolean isDuplicate) {
        SumsubStatus sumsubStatus = null;

        if (isDuplicate) {
            sumsubStatus = SumsubStatus.duplicated;
        } else if (reviewResult != null) {
            // Only set status if reviewResult is non-null
            sumsubStatus = switch (reviewResult) {
                case "GREEN" -> SumsubStatus.success;
                case "RED" -> SumsubStatus.failed;
                default -> SumsubStatus.pending;
            };
        } else {
            // Do not proceed if reviewResult is null and not a duplicate
            return ResponseEntity.ok("No review result yet — skipping DB update.");
        }

        if (user instanceof Freelancer freelancer) {
            freelancer.setSumsubApplicantId(applicantId);
            freelancer.setSumsubStatus(sumsubStatus);
            freelancer.setCompletedIdentity(sumsubStatus == SumsubStatus.success);
            freelancerRepository.save(freelancer);
            return ResponseEntity.ok("Freelancer updated: " + sumsubStatus);

        } else if (user instanceof Employer employer) {
            employer.setSumsubApplicantId(applicantId);
            employer.setSumsubStatus(sumsubStatus);
            employer.setCompletedIdentity(sumsubStatus == SumsubStatus.success);
            employerRepository.save(employer);
            return ResponseEntity.ok("Employer updated: " + sumsubStatus);
        }

        return ResponseEntity.status(400).body("Invalid user type");
    }


    @GetMapping("/freelancer/status")
    public ResponseEntity<Map<String, Object>> getFreelancerSumsubStatus() {
        String userEmail = AuthUtil.getUserEmail();
        String userId = AuthUtil.getUserId();

        Freelancer freelancer = freelancerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        Map<String, Object> result = new HashMap<>();

        result.put("status", freelancer.getSumsubStatus() != null ? freelancer.getSumsubStatus().name() : "pending");
        result.put("applicantId", freelancer.getSumsubApplicantId());
        result.put("completedIdentity", freelancer.isCompletedIdentity());
        result.put("email", userEmail);
        result.put("userId", userId);

        return ResponseEntity.ok(result);
    }



    @GetMapping("/employer/status")
    public ResponseEntity<Map<String, Object>> getEmployerSumsubStatus() {
        String userEmail = AuthUtil.getUserEmail();
        String userId = AuthUtil.getUserId();

        Employer employer = employerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Employer not found"));

        Map<String, Object> result = new HashMap<>();

        result.put("status", employer.getSumsubStatus() != null ? employer.getSumsubStatus().name() : "pending");
        result.put("applicantId", employer.getSumsubApplicantId());
        result.put("completedIdentity", employer.isCompletedIdentity());
        result.put("email", userEmail);
        result.put("userId", userId);

        return ResponseEntity.ok(result);
    }

}
