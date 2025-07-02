package com.giguniverse.backend.Auth.Service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import com.giguniverse.backend.Auth.Repository.AdminRepository;

@Service
public class AdminAccountCleanupService {

    private final AdminRepository adminRepository;

    public AdminAccountCleanupService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Scheduled(fixedRate = 60000) 
    @Transactional
    public void clearExpiredResetTokens() {
        LocalDateTime expiry = LocalDateTime.now().minusMinutes(5);
        adminRepository.clearExpiredResetTokens(expiry);
    }
}
