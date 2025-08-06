package com.giguniverse.backend.WebsocketConfiguration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.giguniverse.backend.Auth.Repository.AdminRepository;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;

@Service
public class OnlineStatusService {

    private final FreelancerRepository freelancerRepo;
    private final EmployerRepository employerRepo;
    private final AdminRepository adminRepo;

    public OnlineStatusService(FreelancerRepository freelancerRepo, EmployerRepository employerRepo, AdminRepository adminRepo) {
        this.freelancerRepo = freelancerRepo;
        this.employerRepo = employerRepo;
        this.adminRepo = adminRepo;
    }

    @Transactional
    public void updateOnlineStatusByRole(String userId, String role, boolean isOnline) {
        switch (role.toLowerCase()) {
            case "freelancer" -> freelancerRepo.findById(userId).ifPresent(freelancer -> {
                freelancer.setOnlineStatus(isOnline);
                freelancerRepo.save(freelancer);
            });
            case "employer" -> employerRepo.findById(userId).ifPresent(employer -> {
                employer.setOnlineStatus(isOnline);
                employerRepo.save(employer);
            });
            case "admin" -> adminRepo.findById(userId).ifPresent(admin -> {
                admin.setOnlineStatus(isOnline);
                adminRepo.save(admin);
            });
            default -> System.out.println("Unknown role: " + role);
        }
    }
}