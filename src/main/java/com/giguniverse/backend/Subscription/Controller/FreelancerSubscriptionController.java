package com.giguniverse.backend.Subscription.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Subscription.Model.SubscriptionDataDTO;
import com.giguniverse.backend.Subscription.Service.FreelancerSubscriptionService;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/freelancer/subscription")
public class FreelancerSubscriptionController {


    @Autowired
    FreelancerSubscriptionService freelancerSubscriptionService;

    @GetMapping("/premium-status")
    public ResponseEntity<Boolean> getPremiumStatus() {
        Boolean premiumStatus = freelancerSubscriptionService.getPremiumStatus();
        return ResponseEntity.ok(premiumStatus);
    }

    @PostMapping("/purchase")
    public ResponseEntity<Map<String, String>> purchaseSubscription(HttpServletRequest request) throws StripeException {
        String userId = AuthUtil.getUserId();
        String email = AuthUtil.getUserEmail();

        Session session = freelancerSubscriptionService.createCheckoutSession(userId, email);

        Map<String, String> response = new HashMap<>();
        response.put("url", session.getUrl());
        return ResponseEntity.ok(response);
    }

    // Cancel Immediately (Testing) 
    @PostMapping("/cancel/immediate")
    public ResponseEntity<?> cancelImmediate() {
        try {
            Subscription cancelled = freelancerSubscriptionService.cancelSubscriptionImmediate();
            return ResponseEntity.ok("Subscription cancelled immediately: " + cancelled.getId());
        } catch (StripeException e) {
            return ResponseEntity.status(500).body("Stripe error: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Cancel at Period End (Production)
    @PostMapping("/cancel/period-end")
    public ResponseEntity<?> cancelAtPeriodEnd() {
        try {
            Subscription cancelled = freelancerSubscriptionService.cancelSubscriptionAtPeriodEnd();
            return ResponseEntity.ok("Subscription will cancel at period end: " + cancelled.getId());
        } catch (StripeException e) {
            return ResponseEntity.status(500).body("Stripe error: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/bill-data")
    public ResponseEntity<List<SubscriptionDataDTO>> getMySubscriptions() {
        List<SubscriptionDataDTO> subscriptions = freelancerSubscriptionService.getMySubscriptions();
        return ResponseEntity.ok(subscriptions);
    }
}
