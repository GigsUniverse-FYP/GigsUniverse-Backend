package com.giguniverse.backend.Profile.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giguniverse.backend.Auth.Model.Admin;
import com.giguniverse.backend.Auth.Repository.AdminRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Profile.Model.AdminProfile;
import com.giguniverse.backend.Profile.Model.DTO.AdminProfileDataResponse;
import com.giguniverse.backend.Profile.Model.DTO.AdminProfileFormData;
import com.giguniverse.backend.Profile.Repository.AdminProfileRepository;
import jakarta.transaction.Transactional;

@Service
public class AdminProfileService {
    private final ObjectMapper mapper = new ObjectMapper();

    public ResponseEntity<Map<String, String>> getCurrentUser() {
        String userId = AuthUtil.getUserId();
        String email = AuthUtil.getUserEmail();

        if (userId == null || email == null) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "User not authenticated"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "email", email
        ));
    }

    private final AdminRepository adminRepo;
    private final AdminProfileRepository pgProfileRepo;

    public AdminProfileService(
        AdminRepository adminRepo,
        AdminProfileRepository pgProfileRepo
    ) {
        this.adminRepo = adminRepo;
        this.pgProfileRepo = pgProfileRepo;
    }

    @Transactional
    public void saveProfile(AdminProfileFormData formData, String userId, String email, MultipartFile profilePicture) throws IOException {

        Admin admin = adminRepo.findByAdminUserId(userId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        AdminProfile pgProfile = pgProfileRepo.findByAdmin(admin)
            .orElseGet(() -> {
                AdminProfile newProfile = new AdminProfile();
                newProfile.setAdmin(admin);
                admin.setProfile(newProfile);
                return newProfile;
            });

        pgProfile.setFullName(formData.getFullName());
        pgProfile.setUsername(formData.getUsername());
        pgProfile.setGender(formData.getGender());
        pgProfile.setDob(LocalDate.parse(formData.getDob()));
        pgProfile.setEmail(email);
        pgProfile.setPhone(formData.getPhone());
        pgProfile.setLocation(formData.getLocation());

        if (profilePicture != null && !profilePicture.isEmpty()) {
            pgProfile.setProfilePicture(profilePicture.getBytes());
            pgProfile.setProfilePictureMimeType(formData.getProfilePictureMimeType());
        } 

        pgProfile.setLanguageProficiency(mapper.writeValueAsString(formData.getLanguageProficiency()));
        pgProfileRepo.save(pgProfile);

        admin.setProfileCompleted(true);
        adminRepo.save(admin);
    }

    public AdminProfileDataResponse getFullAdminProfile() {
        String userId = AuthUtil.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }

        AdminProfile pgProfile = pgProfileRepo.findByAdmin_AdminUserId(userId)
                .orElseThrow(() -> new RuntimeException("Admin profile not found"));

        AdminProfileDataResponse response = new AdminProfileDataResponse();
            response.setAdminProfileId(userId);
            response.setFullName(pgProfile.getFullName());
            response.setUsername(pgProfile.getUsername());
            response.setGender(pgProfile.getGender());
            response.setDob(pgProfile.getDob().toString());
            response.setEmail(pgProfile.getEmail());
            response.setPhone(pgProfile.getPhone());
            response.setLocation(pgProfile.getLocation());
            response.setProfilePicture(Base64.getEncoder().encodeToString(pgProfile.getProfilePicture()));
            response.setProfilePictureMimeType(pgProfile.getProfilePictureMimeType());
            response.setLanguageProficiency(pgProfile.getLanguageProficiency());
        return response;
    }
}
