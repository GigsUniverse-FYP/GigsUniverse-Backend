package com.giguniverse.backend.Auth.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Auth.Model.Admin;


@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {
    boolean existsByEmail(String email);
    boolean existsByAdminUserId(String adminUserId);
    Optional<Admin> findByEmail(String email);
    Optional<Admin> findByAdminUserId(String adminUserId);
    long count();

    @Transactional
    @Modifying
    @Query("""
        UPDATE Admin f 
        SET f.resetPasswordToken = null, f.resetPasswordTokenExpiry = null 
        WHERE f.resetPasswordTokenExpiry IS NOT NULL AND f.resetPasswordTokenExpiry <= :expiryTime
    """)
    void clearExpiredResetTokens(@Param("expiryTime") LocalDateTime expiryTime);

}