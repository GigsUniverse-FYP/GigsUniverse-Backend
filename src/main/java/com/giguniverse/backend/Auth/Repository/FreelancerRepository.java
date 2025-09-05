package com.giguniverse.backend.Auth.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Auth.Model.Freelancer;

@Repository
public interface FreelancerRepository extends JpaRepository<Freelancer, String> {
    boolean existsByEmail(String email);
    boolean existsByFreelancerUserId(String freelancerUserId);
    Optional<Freelancer> findByEmail(String email);
    Optional<Freelancer> findByEmailConfirmationToken(String token);
    Optional<Freelancer> findByFreelancerUserId(String freelancerUserId);
    Optional<Freelancer> findByStripeAccountId(String stripeAccountId);
    long count();
    List<Freelancer> findByRegistrationDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT f.completedOnboarding FROM Freelancer f WHERE f.freelancerUserId = :freelancerUserId")
    Optional<Boolean> isOnboarded(@Param("freelancerUserId") String freelancerUserId);

    // Check the provider based on the use email.
    @Query("SELECT f.registrationProvider FROM Freelancer f WHERE f.email = :email")
    Optional<String> findProviderByEmail(@Param("email") String email);

    @Transactional
    @Modifying
    @Query("""
        UPDATE Freelancer f 
        SET f.resetPasswordToken = null, f.resetPasswordTokenExpiry = null 
        WHERE f.resetPasswordTokenExpiry IS NOT NULL AND f.resetPasswordTokenExpiry <= :expiryTime
    """)
    void clearExpiredResetTokens(@Param("expiryTime") LocalDateTime expiryTime);


    @Transactional
    @Modifying
    @Query("DELETE FROM Freelancer f WHERE f.emailConfirmed = false AND f.registrationDate <= :expiryTime")
    void deleteUnconfirmedAccountsOlderThan(@Param("expiryTime") LocalDateTime expiryTime);
}