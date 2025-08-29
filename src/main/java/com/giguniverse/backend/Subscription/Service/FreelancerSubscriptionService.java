package com.giguniverse.backend.Subscription.Service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Profile.Model.FreelancerProfile;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;
import com.giguniverse.backend.Subscription.Model.FreelancerSubscription;
import com.giguniverse.backend.Subscription.Model.SubscriptionDataDTO;
import com.giguniverse.backend.Subscription.Repository.FreelancerSubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.annotation.PostConstruct;

@Service
public class FreelancerSubscriptionService {

        @Autowired
        FreelancerProfileRepository freelancerProfileRepository;

        @Autowired
        FreelancerSubscriptionRepository freelancerSubscriptionRepository;

        public Boolean getPremiumStatus() {
                String currentUserId = AuthUtil.getUserId();
                FreelancerProfile profile = freelancerProfileRepository.findByFreelancer_FreelancerUserId(currentUserId)
                        .orElseThrow(() -> new RuntimeException("Freelancer profile not found for userId: " + currentUserId));
                return profile.getPremiumStatus() != null ? profile.getPremiumStatus() : false;
        }

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;


    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Value("${stripe.freelancer.subscription.id}")
    private String PREMIUM_FREELANCER_PRICE_ID;

        public Session createCheckoutSession(String userId, String email) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl("https://gigsuniverse.studio/dashboard/freelancer/subscription")
                .setCancelUrl("https://gigsuniverse.studio/dashboard/freelancer/subscription")
                .setCustomerEmail(email)
                .addLineItem(
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPrice(PREMIUM_FREELANCER_PRICE_ID)
                        .build()
                )
                .addAllPaymentMethodType(Arrays.asList(
                SessionCreateParams.PaymentMethodType.CARD
                ))
                .setSubscriptionData(
                SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("userId", userId)
                        .putMetadata("userType", "freelancer")
                        .build()
                )
                .build();

        return Session.create(params);
    }


    public String getLatestSubscriptionIdForCurrentUser() {
        String currentUserId = AuthUtil.getUserId();

        FreelancerSubscription latest= freelancerSubscriptionRepository.findLatestByUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("No subscription found for user: " + currentUserId));

        return latest.getStripeSubscriptionId();
    }


    // Cancel Immediately
    public Subscription cancelSubscriptionImmediate() throws StripeException {
        String subscriptionId = getLatestSubscriptionIdForCurrentUser(); // fetch automatically
        Subscription subscription = Subscription.retrieve(subscriptionId);
        return subscription.cancel();
    }

    // Cancel Period End
    public Subscription cancelSubscriptionAtPeriodEnd() throws StripeException {
        String subscriptionId = getLatestSubscriptionIdForCurrentUser(); 
        Subscription subscription = Subscription.retrieve(subscriptionId);

        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build();

        return subscription.update(params);
    }

    public List<SubscriptionDataDTO> getMySubscriptions() {
        String userId = AuthUtil.getUserId();

        List<FreelancerSubscription> subscriptions = freelancerSubscriptionRepository
                .findAllByFreelancerUserIdOrderByCreatedAtDesc(userId);

        return subscriptions.stream().map(sub -> {
            SubscriptionDataDTO dto = new SubscriptionDataDTO();
            dto.setStripeProductName(sub.getStripeProductName());
            dto.setLatestInvoiceId(sub.getLatestInvoiceId());
            dto.setAmountPaid(sub.getAmountPaid());
            dto.setCurrency(sub.getCurrency());
            dto.setCreatedAt(sub.getCreatedAt());
            dto.setStripeSubscriptionId(sub.getStripeSubscriptionId());
            dto.setStatus(sub.getStatus());
            return dto;
        }).toList();
    }
}
