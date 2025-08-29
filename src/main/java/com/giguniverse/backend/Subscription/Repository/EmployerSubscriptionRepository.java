package com.giguniverse.backend.Subscription.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Subscription.Model.EmployerSubscription;

@Repository
public interface EmployerSubscriptionRepository extends JpaRepository<EmployerSubscription, Integer> {

    Optional<EmployerSubscription> findByEmployerUserId(String employerUserId);

    @Query(value = "SELECT * FROM employer_subscription s WHERE s.employer_user_id = :userId ORDER BY s.created_at DESC LIMIT 1", 
        nativeQuery = true)
    Optional<EmployerSubscription> findLatestByUserId(String userId);

    List<EmployerSubscription> findAllByEmployerUserId(String userId);

    List<EmployerSubscription> findAllByEmployerUserIdOrderByCreatedAtDesc(String userId);
}