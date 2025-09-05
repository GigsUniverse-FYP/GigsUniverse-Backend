package com.giguniverse.backend.Dashboard.Transactions.Model;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionDTO {
    private String subscriptionId;
    private String userId;
    private String userType; // freelancer | employer
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripeProductName;
    private String status;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Long amountPaid;
    private String currency;
    private Instant createdAt;
}
