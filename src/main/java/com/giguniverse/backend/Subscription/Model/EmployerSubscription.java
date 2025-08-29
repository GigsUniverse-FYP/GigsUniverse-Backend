package com.giguniverse.backend.Subscription.Model;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployerSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int employerSubscriptionId;

    // Link to employer
    private String employerUserId;

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
