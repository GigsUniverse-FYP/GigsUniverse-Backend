package com.giguniverse.backend.Subscription.Model;

import java.time.Instant;

import lombok.Data;

@Data
public class SubscriptionDataDTO {
    private String stripeProductName;
    private String latestInvoiceId;
    private Long amountPaid;
    private String currency;
    private Instant createdAt;
    private String stripeSubscriptionId;
    private String status;
}
 