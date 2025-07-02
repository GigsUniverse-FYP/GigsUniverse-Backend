package com.giguniverse.backend.Auth.Service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.Repository.EmployerRepository;

import jakarta.transaction.Transactional;

@Service
public class EmployerAccountCleanupService {

    private final EmployerRepository employerRepository;

    public EmployerAccountCleanupService(EmployerRepository employerRepository) {
        this.employerRepository = employerRepository;
    }

    @Scheduled(fixedRate = 60000)
    public void deleteUnconfirmedAccounts() {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        employerRepository.deleteUnconfirmedAccountsOlderThan(tenMinutesAgo);
    }

    @Scheduled(fixedRate = 60000) 
    @Transactional
    public void clearExpiredResetTokens() {
        LocalDateTime expiry = LocalDateTime.now().minusMinutes(5);
        employerRepository.clearExpiredResetTokens(expiry);
    }
}
