package com.giguniverse.backend.Transaction.Webhook;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.giguniverse.backend.Transaction.Model.Transaction;
import com.giguniverse.backend.Transaction.Repository.TransactionRepository;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/stripe/webhook")
public class TopUpWebhookController {

    private final TransactionRepository transactionRepository;
    private final EmployerProfileRepository employerProfileRepository;

    @Value("${stripe.webhook.topup.secret}")
    private String endpointSecret;

    public TopUpWebhookController(TransactionRepository transactionRepository,
                                  EmployerProfileRepository employerProfileRepository) {
        this.transactionRepository = transactionRepository;
        this.employerProfileRepository = employerProfileRepository;
    }

    @PostMapping("/topup")
    public ResponseEntity<String> handleTopUpWebhook(@RequestBody String payload,
                                                     @RequestHeader("Stripe-Signature") String sigHeader) {
        log.info("➡️ Incoming Stripe webhook: payload length={}, signature={}", payload.length(), sigHeader);

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            System.out.println("✅ Webhook verified. Event type={}" + event.getType());
        } catch (Exception e) {
            System.out.println("❌ Invalid webhook signature");
            return ResponseEntity.badRequest().body("Invalid webhook signature");
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> {
                System.out.println("⚡ Handling checkout.session.completed");
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                Session session;

                if (deserializer.getObject().isPresent() && deserializer.getObject().get() instanceof Session s) {
                    session = s;
                } else {
                    JsonObject raw = JsonParser.parseString(deserializer.getRawJson()).getAsJsonObject();
                    try {
                        session = Session.retrieve(raw.get("id").getAsString());
                        System.out.println("Retrieved session manually: " + session.getId());
                    } catch (StripeException e) {
                        System.out.println("❌ Failed to retrieve Session");
                        return ResponseEntity.internalServerError().body("Failed to retrieve Session");
                    }
                }

                System.out.println("Session ID={}, PaymentIntent={}, Metadata={}" + 
                         session.getId() + session.getPaymentIntent() + session.getMetadata());

                // Save pending transaction
                Optional<Transaction> existing = transactionRepository.findByStripeCheckoutSessionId(session.getId());
                if (existing.isEmpty()) {
                    Transaction tx = Transaction.builder()
                            .employerUserId(session.getMetadata().get("employerUserId")) // from session
                            .stripeCheckoutSessionId(session.getId())
                            .stripePaymentIntentId(session.getPaymentIntent())
                            .amount(session.getAmountTotal())
                            .currency(session.getCurrency())
                            .status("pending")
                            .paymentMethodType("checkout")
                            .paymentType("Top-Up")
                            .description("Employer Top-Up")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    transactionRepository.save(tx);
                    System.out.println("💾 Saved new pending Transaction: " + tx);
                } else {
                    System.out.println("Transaction for session " + session.getId() + " already exists. Skipping insert.");
                }
            }

            case "payment_intent.succeeded" -> {
                System.out.println("⚡ Handling payment_intent.succeeded");
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                PaymentIntent pi;

                if (deserializer.getObject().isPresent() && deserializer.getObject().get() instanceof PaymentIntent p) {
                    pi = p;
                } else {
                    JsonObject raw = JsonParser.parseString(deserializer.getRawJson()).getAsJsonObject();
                    try {
                        pi = PaymentIntent.retrieve(raw.get("id").getAsString());
                        System.out.println("Retrieved PaymentIntent manually: " + pi.getId());
                    } catch (StripeException e) {
                        System.out.println("❌ Failed to retrieve PaymentIntent");
                        return ResponseEntity.internalServerError().body("Failed to retrieve PaymentIntent");
                    }
                }

                System.out.println("PaymentIntent ID=" + pi.getId() + ", Amount=" + pi.getAmount() + ", Currency=" + pi.getCurrency() + ", Status=" + pi.getStatus());

                // Use Transaction row to get employerUserId
                Transaction tx = transactionRepository.findByStripePaymentIntentId(pi.getId())
                        .orElse(null);

                if (tx != null) {
                    log.info("Found existing transaction {} → updating to SUCCESS", tx.getId());
                    tx.setStatus("success");
                    tx.setPaymentMethodType(pi.getPaymentMethodTypes().isEmpty() ? "unknown" : pi.getPaymentMethodTypes().get(0));
                    tx.setUpdatedAt(Instant.now());
                    transactionRepository.save(tx);

                    // Get employerId from transaction row instead of PI metadata
                    String employerId = tx.getEmployerUserId();
                    if (employerId == null) {
                        log.error("❌ employerUserId missing in Transaction row. Credits not updated.");
                    } else {
                        employerProfileRepository.findByEmployer_EmployerUserId(employerId)
                                .ifPresentOrElse(profile -> {
                                    long current = profile.getAvailableCredits() == null ? 0 : profile.getAvailableCredits();
                                    long newCredits = current + pi.getAmount();
                                    profile.setAvailableCredits(newCredits);
                                    employerProfileRepository.save(profile);
                                    System.out.println("💰 Updated credits for employerId=" + employerId + " from " + current + " → " + newCredits);
                                }, () -> {
                                    System.out.println("❌ No EmployerProfile found for employerUserId=" + employerId);
                                });
                    }
                } else {
                    System.out.println("❌ No Transaction found for PaymentIntent " + pi.getId());
                }
            }

            case "payment_intent.payment_failed" -> {
                System.out.println("⚡ Handling payment_intent.payment_failed");
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                PaymentIntent pi;

                if (deserializer.getObject().isPresent() && deserializer.getObject().get() instanceof PaymentIntent p) {
                    pi = p;
                } else {
                    JsonObject raw = JsonParser.parseString(deserializer.getRawJson()).getAsJsonObject();
                    try {
                        pi = PaymentIntent.retrieve(raw.get("id").getAsString());
                        System.out.println("Retrieved PaymentIntent manually: " + pi.getId());
                    } catch (StripeException e) {
                        System.out.println("❌ Failed to retrieve PaymentIntent");
                        return ResponseEntity.internalServerError().body("Failed to retrieve PaymentIntent");
                    }
                }

                System.out.println("PaymentIntent FAILED: ID=" + pi.getId());

                Transaction tx = transactionRepository.findByStripePaymentIntentId(pi.getId())
                        .orElse(null);

                if (tx != null) {
                    tx.setStatus("failed");
                    tx.setPaymentMethodType(pi.getPaymentMethodTypes().isEmpty() ? "unknown" : pi.getPaymentMethodTypes().get(0));
                    tx.setUpdatedAt(Instant.now());
                    transactionRepository.save(tx);
                    System.out.println("🚨 Updated transaction " + tx.getId() + " → FAILED");
                } else {
                    System.out.println("❌ No Transaction found for failed PaymentIntent " + pi.getId());
                }
            }

            default -> {
                System.out.println("⚠️ Unhandled event type: " + event.getType());
                return ResponseEntity.ok("Unhandled event type: " + event.getType());
            }
        }

        return ResponseEntity.ok("Webhook processed");
    }
}
