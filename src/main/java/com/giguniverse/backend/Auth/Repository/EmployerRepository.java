package com.giguniverse.backend.Auth.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Auth.Model.Employer;

@Repository
public interface EmployerRepository extends JpaRepository<Employer, String> {
    boolean existsByEmail(String email);
    boolean existsByEmployerUserId(String employerUserId);
    Optional<Employer> findByEmail(String email);
    Optional<Employer> findByEmailConfirmationToken(String token);

    @Query("SELECT f.registrationProvider FROM Employer f WHERE f.email = :email")
    Optional<String> findProviderByEmail(@Param("email") String email);

    @Transactional
    @Modifying
    @Query("""
        UPDATE Employer f 
        SET f.resetPasswordToken = null, f.resetPasswordTokenExpiry = null 
        WHERE f.resetPasswordTokenExpiry IS NOT NULL AND f.resetPasswordTokenExpiry <= :expiryTime
    """)
    void clearExpiredResetTokens(@Param("expiryTime") LocalDateTime expiryTime);

    @Transactional
    @Modifying
    @Query("DELETE FROM Employer f WHERE f.emailConfirmed = false AND f.registrationDate <= :expiryTime")
    void deleteUnconfirmedAccountsOlderThan(@Param("expiryTime") LocalDateTime expiryTime);
}