package com.giguniverse.backend.Subscription.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int freelancerSubscriptionId;

    // Link to freelancer
    private String freelancerUserId;

    // Stripe identifiers
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripePriceId;
    private String stripeProductId;
    private String stripeProductName;

    // Billing info
    private String status;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private boolean cancelAtPeriodEnd;

    // Latest payment
    private String latestInvoiceId;
    private Long amountPaid;
    private String currency;

    // Metadata
    private Instant createdAt;
    private Instant updatedAt;
}
