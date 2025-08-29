package com.giguniverse.backend.Transaction.Repository;

import com.giguniverse.backend.Transaction.Model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<Transaction> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    List<Transaction> findAllByEmployerUserIdOrderByCreatedAtDesc(String employerUserId);
}
