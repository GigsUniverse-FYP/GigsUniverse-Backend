package com.giguniverse.backend.Auth.Controller;

import com.giguniverse.backend.Auth.JWT.JwtConfig;
import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Auth.Model.Admin;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Profile.Model.AdminProfile;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Model.FreelancerProfile;
import com.giguniverse.backend.Profile.Repository.AdminProfileRepository;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;
import com.giguniverse.backend.Auth.Repository.AdminRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/auth")
public class AllAuthJwtController {

    @Value("${frontend.url}")
    private String frontendUrl;

    private final JwtConfig jwtConfig;
    private final FreelancerRepository freelancerRepository;
    private final EmployerRepository employerRepository;
    private final AdminRepository adminRepository;

    public AllAuthJwtController(
            JwtConfig jwtConfig,
            FreelancerRepository freelancerRepository,
            EmployerRepository employerRepository,
            AdminRepository adminRepository
    ) {
        this.jwtConfig = jwtConfig;
        this.freelancerRepository = freelancerRepository;
        this.employerRepository = employerRepository;
        this.adminRepository = adminRepository;
    }

    @GetMapping("/verify-token")
    public ResponseEntity<?> verifyToken(@CookieValue(name = "jwt", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing token"));
        }

        System.out.println("Received token: " + token);

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String role = (String) claims.get("role");
            String userId = (String) claims.get("userId");
            String email = (String) claims.get("email");

            switch (role) {
                case "freelancer":
                    Freelancer freelancer = freelancerRepository.findById(userId).orElse(null);

                    if (freelancer == null) {
                        return ResponseEntity.status(404).body(Map.of("error", "Freelancer not found"));
                    }
                    if (freelancer.isAccountBannedStatus()) {
                        return ResponseEntity.status(403).body(Map.of(
                            "error", "Account is banned",
                            "reason", Optional.ofNullable(freelancer.getBannedReason()).orElse("Not specified"),
                            "unbanDate", Optional.ofNullable(freelancer.getUnbanDate()).map(Object::toString).orElse("")
                        ));
                    }
                    break;

                case "employer":
                    Employer employer = employerRepository.findById(userId).orElse(null);
                    if (employer == null) {
                        return ResponseEntity.status(404).body(Map.of("error", "Employer not found"));
                    }
                    if (employer.isAccountBannedStatus()) {
                        return ResponseEntity.status(403).body(Map.of(
                                "error", "Account is banned",
                                "reason", Optional.ofNullable(employer.getBannedReason()).orElse("Not specified"),
                                "unbanDate", Optional.ofNullable(employer.getUnbanDate()).map(Object::toString).orElse("")
                        ));
                    }
                    break;

                case "admin":
                    Admin admin = adminRepository.findById(userId).orElse(null);
                    if (admin == null) {
                        return ResponseEntity.status(404).body(Map.of("error", "Admin not found"));
                    }
                    break;

                default:
                    return ResponseEntity.status(400).body(Map.of("error", "Invalid role"));
            }

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "email", email,
                    "role", role
            ));

        } catch (Exception e) {
            e.printStackTrace();  
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .domain(".gigsuniverse.studio")  
                .build();


        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok("Logged out successfully");
    }

    @Autowired
    FreelancerProfileRepository freelancerProfileRepository;
    
    @Autowired
    EmployerProfileRepository employerProfileRepository;

    @Autowired
    AdminProfileRepository adminProfileRepository;


    @GetMapping("/freelancer/nav-info")
    public ResponseEntity<?> getFreelancerNavInfo() {
        String userId = AuthUtil.getUserId();

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        Freelancer freelancer = freelancerRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Freelancer not found"));

        FreelancerProfile freelancerProfile = freelancerProfileRepository.findByFreelancer_FreelancerUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Freelancer profile not found"));



        return ResponseEntity.ok(Map.of(
            "userId", freelancer.getFreelancerUserId(),
            "email", freelancer.getEmail(),
            "username", freelancerProfile.getUsername(),
            "isPremium", freelancerProfile.getPremiumStatus(),
            "profilePicture", Base64.getEncoder().encodeToString(freelancerProfile.getProfilePicture()),
            "profilePictureMimeType", freelancerProfile.getProfilePictureMimeType()
        ));
    }


    @GetMapping("/employer/nav-info")
    public ResponseEntity<?> getEmployerNavInfo() {
        String userId = AuthUtil.getUserId();

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        Employer employer = employerRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Employer not found"));

        EmployerProfile employerProfile = employerProfileRepository.findByEmployer_EmployerUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Employer profile not found"));


        return ResponseEntity.ok(Map.of(
            "userId", employer.getEmployerUserId(),
            "email", employer.getEmail(),
            "username", employerProfile.getUsername(),
            "isPremium", employerProfile.getPremiumStatus(),
            "profilePicture", Base64.getEncoder().encodeToString(employerProfile.getProfilePicture()),
            "profilePictureMimeType", employerProfile.getProfilePictureMimeType()
        ));
    }        

    @GetMapping("/admin/nav-info")
    public ResponseEntity<?> getAdminNavInfo() {
        String userId = AuthUtil.getUserId();

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        Admin admin = adminRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        AdminProfile adminProfile = adminProfileRepository.findByAdmin_AdminUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Admin profile not found"));


        return ResponseEntity.ok(Map.of(
            "userId", admin.getAdminUserId(),
            "email", admin.getEmail(),
            "username", adminProfile.getUsername(),
            "profilePicture", Base64.getEncoder().encodeToString(adminProfile.getProfilePicture()),
            "profilePictureMimeType", adminProfile.getProfilePictureMimeType()
        ));
    }    

}
