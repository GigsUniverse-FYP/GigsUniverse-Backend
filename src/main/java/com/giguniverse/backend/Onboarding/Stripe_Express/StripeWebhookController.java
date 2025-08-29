package com.giguniverse.backend.Onboarding.Stripe_Express;

import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
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

        // Handle v2 version
        if ("v2.account.updated".equals(event.getType())) {
            System.out.println("Processing v2.account.updated event");

            StripeObject stripeObject = event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (stripeObject == null) return ResponseEntity.ok("");

            Account account = (Account) stripeObject;
            processAccountUpdate(account);
        }

        // Backward compatibility for v1
        if ("account.updated".equals(event.getType())) {
            System.out.println("Processing legacy account.updated event");

            Account account = (Account) event.getDataObjectDeserializer().getObject().orElse(null);
            if (account == null) return ResponseEntity.ok("");

            processAccountUpdate(account);
        }

        return ResponseEntity.ok("");
    }

    private void processAccountUpdate(Account account) {
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

            // Fix: handle disabledReason properly
            String disabledReason = account.getRequirements() != null ? account.getRequirements().getDisabledReason() : null;

            if (disabledReason != null && disabledReason.startsWith("rejected")) {
                // Only mark as failed if Stripe explicitly rejected
                freelancer.setStripeStatus(StripeStatus.failed);
            } else if (detailsSubmitted && payoutsEnabled) {
                // Onboarding fully complete
                freelancer.setStripeStatus(StripeStatus.success);
            } else {
                // Still in progress
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
        wsPayload.put("payoutsEnabled", true);

        System.out.println("[Test] Sending WebSocket message to: " + email);
        messagingTemplate.convertAndSendToUser(email, "/queue/stripe-status", wsPayload);

        return "WebSocket message sent to: " + email;
    }
}
