package com.giguniverse.backend.Onboarding.Stripe_Express;

public class StripeOnboardingResponse {
    private String accountId;
    private String onboardingUrl;

    public StripeOnboardingResponse(String accountId, String onboardingUrl) {
        this.accountId = accountId;
        this.onboardingUrl = onboardingUrl;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getOnboardingUrl() {
        return onboardingUrl;
    }
}