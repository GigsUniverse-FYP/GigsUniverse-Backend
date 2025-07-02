package com.giguniverse.backend.Auth.Service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.Repository.FreelancerRepository;

import jakarta.transaction.Transactional;

@Service
public class FreelancerAccountCleanupService {

    private final FreelancerRepository freelancerRepository;

    public FreelancerAccountCleanupService(FreelancerRepository freelancerRepository) {
        this.freelancerRepository = freelancerRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deleteUnconfirmedAccounts() {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        freelancerRepository.deleteUnconfirmedAccountsOlderThan(tenMinutesAgo);
    }

    @Scheduled(fixedRate = 60000) 
    @Transactional
    public void clearExpiredResetTokens() {
        LocalDateTime expiry = LocalDateTime.now().minusMinutes(5);
        freelancerRepository.clearExpiredResetTokens(expiry);
    }
}
