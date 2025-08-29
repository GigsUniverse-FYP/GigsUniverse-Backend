package com.giguniverse.backend.Subscription.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Subscription.Model.FreelancerSubscription;

@Repository
public interface FreelancerSubscriptionRepository extends JpaRepository<FreelancerSubscription, Integer> {

    Optional<FreelancerSubscription> findByFreelancerUserId(String freelancerUserId);


    @Query(value = "SELECT * FROM freelancer_subscription s WHERE s.freelancer_user_id = :userId ORDER BY s.created_at DESC LIMIT 1", 
        nativeQuery = true)
    Optional<FreelancerSubscription> findLatestByUserId(String userId);

    List<FreelancerSubscription> findAllByFreelancerUserId(String userId);

    List<FreelancerSubscription> findAllByFreelancerUserIdOrderByCreatedAtDesc(String userId);
}