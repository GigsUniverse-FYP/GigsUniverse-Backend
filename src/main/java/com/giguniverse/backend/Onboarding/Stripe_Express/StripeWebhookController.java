package com.giguniverse.backend.Onboarding.Stripe_Express;

import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.net.Webhook;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.giguniverse.backend.Auth.Model.StripeStatus;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final SimpMessagingTemplate messagingTemplate;
    private final FreelancerRepository freelancerRepository;

    public StripeWebhookController(SimpMessagingTemplate messagingTemplate, FreelancerRepository freelancerRepository) {
        this.messagingTemplate = messagingTemplate;
        this.freelancerRepository = freelancerRepository;
    }


    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            System.out.println("Invalid webhook signature: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        System.out.println("Stripe event received: " + event.getType());

        if ("account.updated".equals(event.getType())) {
            System.out.println("Processing account.updated event");
            Account account = (Account) event.getDataObjectDeserializer().getObject().orElse(null);
            if (account == null) return ResponseEntity.ok("");

            String accountId = account.getId();
            boolean detailsSubmitted = Boolean.TRUE.equals(account.getDetailsSubmitted());
            boolean payoutsEnabled = Boolean.TRUE.equals(account.getPayoutsEnabled());

            System.out.printf("Stripe Account: %s | detailsSubmitted: %s | payoutsEnabled: %s%n",
                    accountId, detailsSubmitted, payoutsEnabled);

            freelancerRepository.findByStripeAccountId(accountId).ifPresentOrElse(freelancer -> {
                String email = freelancer.getEmail();
                System.out.println("Found freelancer: " + email);

                freelancer.setCompletedPaymentSetup(detailsSubmitted);
                freelancer.setPayOutEnabled(payoutsEnabled);

                if (!account.getRequirements().getDisabledReason().isEmpty()) {
                    freelancer.setStripeStatus(StripeStatus.failed);
                } else if (detailsSubmitted && payoutsEnabled) {
                    freelancer.setStripeStatus(StripeStatus.success);
                } else {
                    freelancer.setStripeStatus(StripeStatus.pending);
                }

                freelancerRepository.save(freelancer);

                Map<String, Object> wsPayload = new HashMap<>();
                wsPayload.put("stripeStatus", freelancer.getStripeStatus().name());
                wsPayload.put("completedPaymentSetup", detailsSubmitted);
                wsPayload.put("payoutsEnabled", payoutsEnabled);

                System.out.println("Sending WebSocket update to: " + email);
                messagingTemplate.convertAndSendToUser(email, "/queue/stripe-status", wsPayload);
            }, () -> {
                System.out.println("No freelancer found with Stripe account ID: " + accountId);
            });
        }

        return ResponseEntity.ok("");
    }


    @PostMapping("/stripe-status")
    public String sendStripeStatusUpdate() {

        String email = AuthUtil.getUserEmail();

        var freelancerOpt = freelancerRepository.findByEmail(email);
        if (freelancerOpt.isEmpty()) {
            return "Freelancer not found: " + email;
        }
        var freelancer = freelancerOpt.get();

        freelancer.setCompletedPaymentSetup(true);
        freelancer.setPayOutEnabled(false);
        freelancer.setStripeStatus(StripeStatus.success); 

        freelancerRepository.save(freelancer);

        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("stripeStatus", "success");
        wsPayload.put("completedPaymentSetup", true);
        wsPayload.put("payoutsEnabled", false);

        System.out.println("[Test] Sending WebSocket message to: " + email);
        messagingTemplate.convertAndSendToUser(email, "/queue/stripe-status", wsPayload);

        return " WebSocket message sent to: " + email;
    }


}
