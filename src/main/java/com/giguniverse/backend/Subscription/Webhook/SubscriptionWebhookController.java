package com.giguniverse.backend.Subscription.Webhook;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;
import com.giguniverse.backend.Subscription.Model.EmployerSubscription;
import com.giguniverse.backend.Subscription.Model.FreelancerSubscription;
import com.giguniverse.backend.Subscription.Repository.EmployerSubscriptionRepository;
import com.giguniverse.backend.Subscription.Repository.FreelancerSubscriptionRepository;
import com.stripe.net.Webhook;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.Product;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RestController
@RequestMapping("/api/stripe/webhook")
public class SubscriptionWebhookController {

    @Value("${stripe.webhook.subscribe.secret}")
    private String endpointSecret;

    @Autowired
    private FreelancerSubscriptionRepository freelancerSubscriptionRepository;

    @Autowired
    private EmployerSubscriptionRepository employerSubscriptionRepository;

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @PostMapping("/subscribe")
    public String handleStripeWebhook(@RequestBody String payload,
                                      @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        System.out.println("Received webhook payload");

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            System.out.println("Webhook signature verified successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Webhook signature verification failed");
            return "Webhook signature verification failed.";
        }

        System.out.println("Event type: " + event.getType());

        try {
            switch (event.getType()) {
                case "invoice.payment_succeeded", "customer.subscription.created", "customer.subscription.updated" ->
                        handleInvoicePaymentSucceeded(event);
                case "customer.subscription.deleted" ->
                        handleSubscriptionDeleted(event);
                default ->
                        System.out.println("Unhandled event type: " + event.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing webhook: " + e.getMessage();
        }

        return "Event handled: " + event.getType();
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Invoice invoice = null;

            if (deserializer.getObject().isPresent() && deserializer.getObject().get() instanceof Invoice inv) {
                invoice = inv;
            } else {
                JsonObject raw = JsonParser.parseString(event.getDataObjectDeserializer().getRawJson()).getAsJsonObject();
                String invoiceId = raw.get("id").getAsString();
                invoice = Invoice.retrieve(invoiceId);
            }

            if (invoice == null) {
                System.err.println("❌ Could not retrieve invoice from event.");
                return;
            }

            System.out.println("Invoice retrieved: " + invoice.getId());

            String subscriptionId = invoice.getSubscription();
            if (subscriptionId == null) {
                System.err.println("Invoice has no subscription ID: " + invoice.getId());
                return;
            }

            Subscription subscription = Subscription.retrieve(subscriptionId);
            handleSubscriptionUpdate(invoice, subscription);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSubscriptionUpdate(Invoice invoice, Subscription subscription) {
        System.out.println("Handling subscription update: " + subscription.getId());

        String customerId = subscription.getCustomer();
        String status = subscription.getStatus();
        System.out.println("Customer ID: " + customerId + ", status: " + status);

        String userType = null;
        String userId = null;

        // try invoice line metadata
        if (invoice.getLines() != null && !invoice.getLines().getData().isEmpty()) {
            InvoiceLineItem firstLineItem = invoice.getLines().getData().get(0);
            Map<String, String> metadata = firstLineItem.getMetadata();
            if (metadata != null && !metadata.isEmpty()) {
                userType = metadata.get("userType");
                userId = metadata.get("userId");
            }
        }

        // fallback to subscription metadata
        if ((userType == null || userId == null) && subscription.getMetadata() != null) {
            Map<String, String> metadata = subscription.getMetadata();
            userType = metadata.get("userType");
            userId = metadata.get("userId");
        }

        System.out.println("Metadata - userType: " + userType + ", userId: " + userId);

        if (userType == null || userId == null) {
            System.err.println("Missing metadata in invoice/subscription: " + invoice.getId());
            return;
        }

        Instant currentPeriodStart = Instant.ofEpochSecond(invoice.getPeriodStart());
        Instant currentPeriodEnd = Instant.ofEpochSecond(invoice.getPeriodEnd());

        try {
            String productId = subscription.getItems().getData().get(0).getPrice().getProduct();
            Product product = Product.retrieve(productId); // fetch full product
            String productName = product.getName();

            if ("freelancer".equals(userType)) {
                saveFreelancerSubscription(invoice, subscription, userId, customerId, status,
                        currentPeriodStart, currentPeriodEnd, productId, productName);
            } else if ("employer".equals(userType)) {
                saveEmployerSubscription(invoice, subscription, userId, customerId, status,
                        currentPeriodStart, currentPeriodEnd, productId, productName);
            } else {
                System.out.println("Unknown userType: " + userType);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed retrieving product for subscription " + subscription.getId());
        }
    }

    private void saveFreelancerSubscription(Invoice invoice, Subscription subscription,
                                            String userId, String customerId, String status,
                                            Instant currentPeriodStart, Instant currentPeriodEnd,
                                            String productId, String productName) {
        System.out.println("Saving freelancer subscription for user: " + userId);

        FreelancerSubscription sub = FreelancerSubscription.builder()
                .freelancerUserId(userId)
                .stripeCustomerId(customerId)
                .stripeSubscriptionId(subscription.getId())
                .stripePriceId(subscription.getItems().getData().get(0).getPrice().getId())
                .stripeProductId(productId)
                .stripeProductName(productName)
                .status(status)
                .currentPeriodStart(currentPeriodStart)
                .currentPeriodEnd(currentPeriodEnd)
                .cancelAtPeriodEnd(subscription.getCancelAtPeriodEnd())
                .latestInvoiceId(invoice.getId())
                .amountPaid(invoice.getAmountPaid())
                .currency(invoice.getCurrency())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        freelancerSubscriptionRepository.save(sub);
        System.out.println("Freelancer subscription saved");

        freelancerProfileRepository.findByFreelancer_FreelancerUserId(userId).ifPresentOrElse(profile -> {
            profile.setPremiumStatus(true);
            freelancerProfileRepository.save(profile);
            System.out.println("Freelancer profile updated with premiumStatus=true");
        }, () -> System.err.println("No freelancer profile found for userId: " + userId));
    }

    private void saveEmployerSubscription(Invoice invoice, Subscription subscription,
                                          String userId, String customerId, String status,
                                          Instant currentPeriodStart, Instant currentPeriodEnd,
                                          String productId, String productName) {
        System.out.println("Saving employer subscription for user: " + userId);

        EmployerSubscription sub = EmployerSubscription.builder()
                .employerUserId(userId)
                .stripeCustomerId(customerId)
                .stripeSubscriptionId(subscription.getId())
                .stripePriceId(subscription.getItems().getData().get(0).getPrice().getId())
                .stripeProductId(productId)
                .stripeProductName(productName)
                .status(status)
                .currentPeriodStart(currentPeriodStart)
                .currentPeriodEnd(currentPeriodEnd)
                .cancelAtPeriodEnd(subscription.getCancelAtPeriodEnd())
                .latestInvoiceId(invoice.getId())
                .amountPaid(invoice.getAmountPaid())
                .currency(invoice.getCurrency())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        employerSubscriptionRepository.save(sub);
        System.out.println("Employer subscription saved");

        employerProfileRepository.findByEmployer_EmployerUserId(userId).ifPresentOrElse(profile -> {
            profile.setPremiumStatus(true);
            employerProfileRepository.save(profile);
            System.out.println("Employer profile updated with premiumStatus=true");
        }, () -> System.err.println("No employer profile found for userId: " + userId));
    }

    private void handleSubscriptionDeleted(Event event) {
        Subscription subscription = null;

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent() && deserializer.getObject().get() instanceof Subscription sub) {
            subscription = sub;
        } else {
            try {
                JsonObject raw = JsonParser.parseString(deserializer.getRawJson()).getAsJsonObject();
                String subscriptionId = raw.get("id").getAsString();
                subscription = Subscription.retrieve(subscriptionId);
            } catch (Exception e) {
                System.err.println("Failed to retrieve subscription from raw JSON: " + e.getMessage());
                return;
            }
        }

        if (subscription == null) {
            System.out.println("Subscription delete event object is null");
            return;
        }

        Map<String, String> metadata = subscription.getMetadata();
        if (metadata == null) {
            System.out.println("Subscription metadata is null");
            return;
        }

        String userType = metadata.get("userType");
        String userId = metadata.get("userId");

        System.out.println("Subscription deleted - userType: " + userType + ", userId: " + userId);

        if (userType == null || userId == null) return;

        if ("freelancer".equals(userType)) {
            freelancerProfileRepository.findByFreelancer_FreelancerUserId(userId).ifPresentOrElse(profile -> {
                profile.setPremiumStatus(false);
                freelancerProfileRepository.save(profile);
                System.out.println("Freelancer profile downgraded.");
            }, () -> System.err.println("No freelancer profile found for userId: " + userId));
        } else if ("employer".equals(userType)) {
            employerProfileRepository.findByEmployer_EmployerUserId(userId).ifPresentOrElse(profile -> {
                profile.setPremiumStatus(false);
                employerProfileRepository.save(profile);
                System.out.println("Employer profile downgraded.");
            }, () -> System.err.println("No employer profile found for userId: " + userId));
        }
    }

}
