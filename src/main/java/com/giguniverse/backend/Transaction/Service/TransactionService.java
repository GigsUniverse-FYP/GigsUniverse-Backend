package com.giguniverse.backend.Transaction.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;
import com.giguniverse.backend.Transaction.Model.TopUpRequest;
import com.giguniverse.backend.Transaction.Model.Transaction;
import com.giguniverse.backend.Transaction.Model.TransactionHistory;
import com.giguniverse.backend.Transaction.Repository.TransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;


@Service
public class TransactionService {
    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public Long getAvailableCreditsForCurrentEmployer() {
        String employerUserId = AuthUtil.getUserId();
        EmployerProfile profile = employerProfileRepository.findByEmployer_EmployerUserId(employerUserId)
                .orElseThrow(() -> new RuntimeException("Employer profile not found"));

        return profile.getAvailableCredits() != null ? profile.getAvailableCredits() : 0;
    }

    public String createStripeTopUpSession(TopUpRequest request) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        String email = AuthUtil.getUserEmail();
        String userId = AuthUtil.getUserId();

 
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://gigsuniverse.studio/dashboard/employer/top-up?status=success&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("https://gigsuniverse.studio/dashboard/employer/top-up?status=cancel")
                .setCustomerEmail(email)
                .addPaymentMethodType(toPaymentMethodType(request.getMethod()))
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(request.getAmount()) // amount in cents
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Employer Wallet Top-up")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("method", request.getMethod())
                .putMetadata("employerUserId", userId)
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata("method", request.getMethod())
                                .putMetadata("employerUserId", userId)
                                .build()
                )
                .build();

        Session session = Session.create(params);

        return session.getUrl();
    }

    private SessionCreateParams.PaymentMethodType toPaymentMethodType(String method) {
        return switch (method) {
            case "card", "apple_pay", "google_pay" -> SessionCreateParams.PaymentMethodType.CARD;
            case "us_bank_account" -> SessionCreateParams.PaymentMethodType.US_BANK_ACCOUNT;
            default -> SessionCreateParams.PaymentMethodType.CARD;
            };
        }

    public List<TransactionHistory> getMyTransactionHistory() {
        String userId = AuthUtil.getUserId();

        List<Transaction> transactions = transactionRepository.findAllByEmployerUserIdOrderByCreatedAtDesc(userId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
                                               .withZone(ZoneId.of("Asia/Kuala_Lumpur"));
                                               
        return transactions.stream().map(tx -> {
            TransactionHistory history = new TransactionHistory();
            history.setId(tx.getId());
            history.setType(tx.getPaymentType());
            history.setAmount(tx.getAmount());
            history.setCurrency(tx.getCurrency());
            history.setMethod(tx.getPaymentMethodType());
            history.setStatus(tx.getStatus());
            history.setDate(formatter.format(tx.getCreatedAt()));
            history.setDescription(tx.getDescription());
            return history;
        }).collect(Collectors.toList());
    }


}
