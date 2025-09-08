package com.giguniverse.backend.Onboarding.Stripe_Express;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Value("${stripe.webhook.express.secret}")
    private String endpointSecret;

    private final SimpMessagingTemplate messagingTemplate;
    private final FreelancerRepository freelancerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public StripeWebhookController(SimpMessagingTemplate messagingTemplate, FreelancerRepository freelancerRepository) {
        this.messagingTemplate = messagingTemplate;
        this.freelancerRepository = freelancerRepository;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        System.out.println("=== Stripe Webhook Fired ===");
        System.out.println("Raw payload: " + payload);
        System.out.println("Stripe-Signature header: " + sigHeader);

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            System.out.println("Webhook signature verified ");
        } catch (Exception e) {
            System.out.println("Invalid webhook signature : " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        System.out.println("Stripe event received: " + event.getType());

        if ("account.updated".equals(event.getType())) {
            System.out.println(">>> Handling account.updated event");

            try {
                // Parse the data.object manually
                JsonNode root = objectMapper.readTree(payload);
                JsonNode dataObject = root.path("data").path("object");

                if (dataObject.isMissingNode()) {
                    System.out.println("data.object missing, ignoring");
                    return ResponseEntity.ok("");
                }

                String accountId = dataObject.path("id").asText(null);
                boolean detailsSubmitted = dataObject.path("details_submitted").asBoolean(false);
                boolean payoutsEnabled = dataObject.path("payouts_enabled").asBoolean(false);
                String disabledReason = null;

                JsonNode requirementsNode = dataObject.path("requirements");
                if (!requirementsNode.isMissingNode()) {
                    disabledReason = requirementsNode.path("disabled_reason").asText(null);
                }

                System.out.printf("Parsed account: %s | detailsSubmitted: %s | payoutsEnabled: %s | disabledReason: %s%n",
                        accountId, detailsSubmitted, payoutsEnabled, disabledReason);

                processAccountUpdate(accountId, detailsSubmitted, payoutsEnabled, disabledReason);

            } catch (Exception e) {
                System.out.println("Failed to parse Stripe payload: " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            System.out.println("Ignoring unsupported event type: " + event.getType());
        }

        return ResponseEntity.ok("");
    }

    private void processAccountUpdate(String accountId, boolean detailsSubmitted, boolean payoutsEnabled, String disabledReason) {
        System.out.println(">>> processAccountUpdate START");

        System.out.printf("Stripe Account: %s | detailsSubmitted: %s | payoutsEnabled: %s%n",
                accountId, detailsSubmitted, payoutsEnabled);

        freelancerRepository.findByStripeAccountId(accountId).ifPresentOrElse(freelancer -> {
            String email = freelancer.getEmail();
            System.out.println("Found freelancer in DB: " + email);

            freelancer.setCompletedPaymentSetup(detailsSubmitted);
            freelancer.setPayOutEnabled(payoutsEnabled);

            System.out.println("Stripe disabledReason: " + disabledReason);

            if (disabledReason != null && disabledReason.startsWith("rejected")) {
                freelancer.setStripeStatus(StripeStatus.failed);
                System.out.println("Stripe status set to FAILED");
            } else if (detailsSubmitted && payoutsEnabled) {
                freelancer.setStripeStatus(StripeStatus.success);
                System.out.println("Stripe status set to SUCCESS");
            } else {
                freelancer.setStripeStatus(StripeStatus.pending);
                System.out.println("Stripe status set to PENDING");
            }

            freelancerRepository.save(freelancer);
            System.out.println("Freelancer saved to DB");

            Map<String, Object> wsPayload = new HashMap<>();
            wsPayload.put("stripeStatus", freelancer.getStripeStatus().name());
            wsPayload.put("completedPaymentSetup", detailsSubmitted);
            wsPayload.put("payoutsEnabled", payoutsEnabled);

            System.out.println("Sending WebSocket update to user: " + email + " with payload: " + wsPayload);
            messagingTemplate.convertAndSendToUser(email, "/queue/stripe-status", wsPayload);

        }, () -> {
            System.out.println("⚠️ No freelancer found in DB with Stripe account ID: " + accountId);
        });

    }

    @PostMapping("/stripe-status")
    public String sendStripeStatusUpdate() {

        String email = AuthUtil.getUserEmail();
        System.out.println("Authenticated email: " + email);

        var freelancerOpt = freelancerRepository.findByEmail(email);
        if (freelancerOpt.isEmpty()) {
            System.out.println("Freelancer not found in DB: " + email);
            return "Freelancer not found: " + email;
        }
        var freelancer = freelancerOpt.get();

        freelancer.setCompletedPaymentSetup(true);
        freelancer.setPayOutEnabled(false);
        freelancer.setStripeStatus(StripeStatus.success);

        freelancerRepository.save(freelancer);
        System.out.println("Test freelancer updated in DB");

        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("stripeStatus", "success");
        wsPayload.put("completedPaymentSetup", true);
        wsPayload.put("payoutsEnabled", true);

        System.out.println("Sending WebSocket message to: " + email + " with payload: " + wsPayload);
        messagingTemplate.convertAndSendToUser(email, "/queue/stripe-status", wsPayload);

        return "WebSocket message sent to: " + email;
    }
}
