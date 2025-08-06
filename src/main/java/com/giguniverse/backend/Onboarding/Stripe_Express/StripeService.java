package com.giguniverse.backend.Onboarding.Stripe_Express;

import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Model.StripeStatus;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    private final FreelancerRepository freelancerRepository;

    public StripeService(FreelancerRepository freelancerRepository) {
        this.freelancerRepository = freelancerRepository;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public StripeOnboardingResponse createExpressAccountAndLink(String userEmail) throws StripeException {
        Freelancer freelancer = freelancerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Freelancer not found for email: " + userEmail));

        String accountId = freelancer.getStripeAccountId();

        if (accountId != null) {
            Account existingAccount = Account.retrieve(accountId);

            if (Boolean.TRUE.equals(existingAccount.getDetailsSubmitted())) {
                freelancer.setStripeStatus(StripeStatus.success);
                freelancer.setCompletedPaymentSetup(true);
                freelancer.setPayOutEnabled(Boolean.TRUE.equals(existingAccount.getPayoutsEnabled()));
                freelancerRepository.save(freelancer);

                return new StripeOnboardingResponse(accountId, null);
            } else if (freelancer.getStripeStatus() == StripeStatus.pending) {
                String onboardingLink = generateOnboardingLink(accountId);
                return new StripeOnboardingResponse(accountId, onboardingLink);
            } else {
                // Possibly failed or unhandled status
                throw new IllegalStateException("Stripe account in unknown or failed state for user: " + userEmail);
            }

        } else {
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setCountry("MY")
                    .setEmail(userEmail)
                    .setCapabilities(
                            AccountCreateParams.Capabilities.builder()
                                    .setCardPayments(
                                            AccountCreateParams.Capabilities.CardPayments.builder().setRequested(true).build()
                                    )
                                    .setTransfers(
                                            AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build()
                                    )
                                    .build()
                    )
                    .build();

            Account newAccount = Account.create(params);
            String newAccountId = newAccount.getId();

            // Save to freelancer
            freelancer.setStripeAccountId(newAccountId);
            freelancer.setStripeStatus(StripeStatus.pending);
            freelancer.setCompletedPaymentSetup(false);
            freelancer.setPayOutEnabled(false);
            freelancerRepository.save(freelancer);

            String onboardingLink = generateOnboardingLink(newAccountId);

            return new StripeOnboardingResponse(newAccountId, onboardingLink);
        }
    }

    private String generateOnboardingLink(String accountId) throws StripeException {
        AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                .setAccount(accountId)
                .setRefreshUrl(frontendUrl + "/dashboard/freelancer/onboarding/auto-close")
                .setReturnUrl(frontendUrl + "/dashboard/freelancer/onboarding/auto-close")
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();

        return AccountLink.create(linkParams).getUrl();
    }

}
