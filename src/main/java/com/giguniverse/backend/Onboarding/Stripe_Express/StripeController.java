package com.giguniverse.backend.Onboarding.Stripe_Express;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.stripe.exception.StripeException;


@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    @Autowired
    private StripeService stripeService;
    @Autowired
    private FreelancerRepository freelancerRepository;

    @PostMapping("/onboarding")
    public ResponseEntity<?> startStripeOnboarding() {
        try {

            String email = AuthUtil.getUserEmail(); 
            StripeOnboardingResponse response = stripeService.createExpressAccountAndLink(email);

            // Save accountId to DB
            Freelancer freelancer = freelancerRepository.findByEmail(email).orElseThrow();
            if (freelancer.getStripeAccountId() == null) {
                freelancer.setStripeAccountId(response.getAccountId());
                freelancerRepository.save(freelancer);
            }

            Map<String, String> responseBody = new HashMap<>();
                if (response.getOnboardingUrl() != null) {
                    responseBody.put("onboardingUrl", response.getOnboardingUrl());
                }
                return ResponseEntity.ok(responseBody);

        } catch (StripeException e) {
            return ResponseEntity.status(500).body("Stripe setup failed: " + e.getMessage());
        }
    }

    @GetMapping("/freelancer/status")
    public ResponseEntity<Map<String, Object>> getFreelancerSumsubStatus() {
        String userEmail = AuthUtil.getUserEmail();
        String userId = AuthUtil.getUserId();

        Freelancer freelancer = freelancerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        Map<String, Object> result = new HashMap<>();

        result.put("status", freelancer.getStripeStatus() != null ? freelancer.getStripeStatus().name() : "pending");
        result.put("applicantId", freelancer.getStripeAccountId());
        result.put("completedPayment", freelancer.isCompletedPaymentSetup());
        result.put("payOutEnabled", freelancer.isPayOutEnabled());
        result.put("email", userEmail);
        result.put("userId", userId);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/express-login")
    public ResponseEntity<?> getExpressLoginLink() {

        String userId = AuthUtil.getUserId();

        Optional<Freelancer> optional = freelancerRepository.findByFreelancerUserId(userId);
        if (optional.isEmpty()) return ResponseEntity.notFound().build();

        try {
            String accountId = optional.get().getStripeAccountId();
            String loginUrl = stripeService.generateLoginLink(accountId);
            return ResponseEntity.ok(Map.of("url", loginUrl));
        } catch (StripeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}


