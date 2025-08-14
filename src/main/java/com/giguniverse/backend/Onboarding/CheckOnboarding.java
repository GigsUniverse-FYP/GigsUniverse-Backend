package com.giguniverse.backend.Onboarding;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Auth.Model.Admin;
import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Repository.AdminRepository;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import org.springframework.web.bind.annotation.PostMapping;



@RestController
@RequestMapping("/api/onboarding")
public class CheckOnboarding {

    @Autowired
    private FreelancerRepository freelancerRepo;

    @Autowired
    private EmployerRepository employerRepo;

    @Autowired
    private AdminRepository adminRepo;

    @GetMapping("/freelancer")
    public ResponseEntity<?> checkFreelancerOnboardingStatus() {
        String userId = AuthUtil.getUserId();

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        boolean isCompleted = freelancerRepo.isOnboarded(userId).orElse(false);

        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "completedOnboarding", isCompleted
        ));
    }

    @GetMapping("/employer")
    public ResponseEntity<?> checkEmployerOnboardingStatus() {
        String userId = AuthUtil.getUserId();

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        
        boolean isCompleted = employerRepo.isOnboarded(userId).orElse(false);

        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "completedOnboarding", isCompleted
        ));
    }

    @GetMapping("/freelancer/status")
    public ResponseEntity<?> getFreelancerOnboardingStatus() {
        String userId = AuthUtil.getUserId(); // pulled from JWT or session

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        Optional<Freelancer> optional = freelancerRepo.findByFreelancerUserId(userId);
        if (optional.isEmpty()) return ResponseEntity.notFound().build();

        Freelancer f = optional.get();

        Map<String, Object> status = new HashMap<>();
        status.put("userId", userId);
        status.put("completedProfile", f.isCompletedProfile());
        status.put("completedIdentity", f.isCompletedIdentity());
        status.put("completedPaymentSetup", f.isCompletedPaymentSetup());
        status.put("completedOnboarding", f.isCompletedOnboarding());

        return ResponseEntity.ok(status);
    }

    @PostMapping("freelancer/onboarded")
    public ResponseEntity<?> onboardFreelancer() {
        String email = AuthUtil.getUserEmail();

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        Optional<Freelancer> optionalFreelancer = freelancerRepo.findByEmail(email);
        if (optionalFreelancer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Freelancer not found");
        }

        Freelancer freelancer = optionalFreelancer.get();
        freelancer.setCompletedOnboarding(true);
        freelancerRepo.save(freelancer);

        return ResponseEntity.ok("Freelancer onboarding marked as complete");
    }
    
    @GetMapping("/employer/status")
    public ResponseEntity<?> getEmployerOnboardingStatus() {
        String userId = AuthUtil.getUserId(); 

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        Optional<Employer> optional = employerRepo.findByEmployerUserId(userId);
        if (optional.isEmpty()) return ResponseEntity.notFound().build();

        Employer e = optional.get();

        Map<String, Object> status = new HashMap<>();
        status.put("userId", userId);
        status.put("completedProfile", e.isCompletedProfile());
        status.put("completedIdentity", e.isCompletedIdentity());
        status.put("completedOnboarding", e.isCompletedOnboarding());

        return ResponseEntity.ok(status);
    }

    @PostMapping("employer/onboarded")
    public ResponseEntity<?> onboardEmployer() {
        String email = AuthUtil.getUserEmail();

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        Optional<Employer> optionalEmployer = employerRepo.findByEmail(email);
        if (optionalEmployer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employer not found");
        }

        Employer employer = optionalEmployer.get();
        employer.setCompletedOnboarding(true);
        employerRepo.save(employer);

        return ResponseEntity.ok("Employer onboarding marked as complete");
    }

    @GetMapping("admin/profile")
    public ResponseEntity<?> getAdminProfile() {
        String userId = AuthUtil.getUserId();

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        Optional<Admin> optional = adminRepo.findByAdminUserId(userId);
        if (optional.isEmpty()) return ResponseEntity.notFound().build();

        Admin admin = optional.get();

        Map<String, Object> profile = new HashMap<>();
        profile.put("profileCompleted", admin.isProfileCompleted());

        return ResponseEntity.ok(profile);
    }
}
