package com.giguniverse.backend.Subscription.Service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;
import com.giguniverse.backend.Subscription.Model.EmployerSubscription;
import com.giguniverse.backend.Subscription.Model.SubscriptionDataDTO;
import com.giguniverse.backend.Subscription.Repository.EmployerSubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.annotation.PostConstruct;

@Service
public class EmployerSubscriptionService {
        @Autowired
        EmployerProfileRepository employerProfileRepository;

        @Autowired
        EmployerSubscriptionRepository employerSubscriptionRepository;

        public Boolean getPremiumStatus() {
                String currentUserId = AuthUtil.getUserId();
                EmployerProfile profile = employerProfileRepository.findByEmployer_EmployerUserId(currentUserId)
                        .orElseThrow(() -> new RuntimeException("Employer profile not found for userId: " + currentUserId));
                return profile.getPremiumStatus() != null ? profile.getPremiumStatus() : false;
        }

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;


    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Value("${stripe.employer.subscription.id}")
    private String PREMIUM_EMPLOYER_PRICE_ID;

        public Session createCheckoutSession(String userId, String email) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl("https://gigsuniverse.studio/dashboard/employer/subscription")
                .setCancelUrl("https://gigsuniverse.studio/dashboard/employer/subscription")
                .setCustomerEmail(email)
                .addLineItem(
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPrice(PREMIUM_EMPLOYER_PRICE_ID)
                        .build()
                )
                .addAllPaymentMethodType(Arrays.asList(
                SessionCreateParams.PaymentMethodType.CARD
                ))
                .setSubscriptionData(
                SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("userId", userId)
                        .putMetadata("userType", "employer")
                        .build()
                )
                .build();

        return Session.create(params);
        }

    public String getLatestSubscriptionIdForCurrentUser() {
        String currentUserId = AuthUtil.getUserId();

        EmployerSubscription latest= employerSubscriptionRepository.findLatestByUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("No subscription found for user: " + currentUserId));

        return latest.getStripeSubscriptionId();
    }

    // Cancel Immediately [Testing Purpose]
    public Subscription cancelSubscriptionImmediate() throws StripeException {
        String subscriptionId = getLatestSubscriptionIdForCurrentUser(); 
        Subscription subscription = Subscription.retrieve(subscriptionId);
        return subscription.cancel();
    }

    // Cancel at Period End [For Production]
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

        List<EmployerSubscription> subscriptions = employerSubscriptionRepository
                .findAllByEmployerUserIdOrderByCreatedAtDesc(userId);

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
