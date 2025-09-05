package com.giguniverse.backend.Dashboard.Users.Service;

import com.giguniverse.backend.Auth.Model.Admin;
import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Repository.AdminRepository;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Dashboard.Users.Model.SuspendUserRequest;
import com.giguniverse.backend.Dashboard.Users.Model.UnsuspendUserRequest;
import com.giguniverse.backend.Dashboard.Users.Model.UserRecordDTO;
import com.giguniverse.backend.Profile.Model.FreelancerProfile;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Model.AdminProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class UserRecordService {

    private final FreelancerRepository freelancerRepository;
    private final EmployerRepository employerRepository;
    private final AdminRepository adminRepository;

    public UserRecordService(FreelancerRepository freelancerRepository,
                             EmployerRepository employerRepository,
                             AdminRepository adminRepository) {
        this.freelancerRepository = freelancerRepository;
        this.employerRepository = employerRepository;
        this.adminRepository = adminRepository;
    }

    public List<UserRecordDTO> getAllUserRecords() {
        List<UserRecordDTO> records = new ArrayList<>();


        for (Freelancer f : freelancerRepository.findAll()) {
            FreelancerProfile p = f.getProfile();
            if (p == null) continue;

            UserRecordDTO dto = new UserRecordDTO();
            dto.setUserId(f.getFreelancerUserId());
            dto.setRole(f.getRole() != null ? f.getRole() : "freelancer");
            dto.setOnlineStatus(f.isOnlineStatus());
            dto.setAccountCreationDate(f.getRegistrationDate());
            dto.setAccountBannedStatus(f.isAccountBannedStatus());
            dto.setBannedReason(f.getBannedReason());
            dto.setUnbanDate(f.getUnbanDate());

            dto.setFullName(p.getFullName());
            dto.setUsername(p.getUsername());
            dto.setEmail(p.getEmail() != null ? p.getEmail() : f.getEmail());
            dto.setPhone(p.getPhone());
            dto.setPremiumStatus(p.getPremiumStatus());

            if (p.getProfilePicture() != null) {
                dto.setProfilePictureBase64(Base64.getEncoder().encodeToString(p.getProfilePicture()));
                dto.setProfilePictureMimeType(p.getProfilePictureMimeType());
            }

            records.add(dto);
        }


        for (Employer e : employerRepository.findAll()) {
            EmployerProfile p = e.getProfile();
            if (p == null) continue;

            UserRecordDTO dto = new UserRecordDTO();
            dto.setUserId(e.getEmployerUserId());
            dto.setRole(e.getRole() != null ? e.getRole() : "employer");
            dto.setOnlineStatus(e.isOnlineStatus());
            dto.setAccountCreationDate(e.getRegistrationDate());
            dto.setAccountBannedStatus(e.isAccountBannedStatus());
            dto.setBannedReason(e.getBannedReason());
            dto.setUnbanDate(e.getUnbanDate());

            dto.setFullName(p.getFullName());
            dto.setUsername(p.getUsername());
            dto.setEmail(p.getEmail() != null ? p.getEmail() : e.getEmail());
            dto.setPhone(p.getPhone());
            dto.setPremiumStatus(p.getPremiumStatus());

            if (p.getProfilePicture() != null) {
                dto.setProfilePictureBase64(Base64.getEncoder().encodeToString(p.getProfilePicture()));
                dto.setProfilePictureMimeType(p.getProfilePictureMimeType());
            }

            records.add(dto);
        }

        for (Admin a : adminRepository.findAll()) {
            AdminProfile p = a.getProfile();
            if (p == null) continue;

            UserRecordDTO dto = new UserRecordDTO();
            dto.setUserId(a.getAdminUserId());
            dto.setRole(a.getRole() != null ? a.getRole() : "admin");
            dto.setOnlineStatus(a.isOnlineStatus());
            dto.setAccountCreationDate(null); 
            dto.setPremiumStatus(null);
            dto.setAccountBannedStatus(false);
            dto.setBannedReason(null);
            dto.setUnbanDate(null);

            dto.setFullName(p.getFullName());
            dto.setUsername(p.getUsername());
            dto.setEmail(a.getEmail() != null ? a.getEmail() : p.getEmail());
            dto.setPhone(p.getPhone());

            if (p.getProfilePicture() != null) {
                dto.setProfilePictureBase64(Base64.getEncoder().encodeToString(p.getProfilePicture()));
                dto.setProfilePictureMimeType(p.getProfilePictureMimeType());
            }

            records.add(dto);
        }

        return records;
    }


    public void suspendUser(SuspendUserRequest request) {
        if ("freelancer".equalsIgnoreCase(request.getRole())) {
            freelancerRepository.findById(request.getUserId()).ifPresent(freelancer -> {
                freelancer.setAccountBannedStatus(true);
                freelancer.setBannedReason(request.getReason());
                freelancer.setUnbanDate(request.getEndDate());
                freelancerRepository.save(freelancer);
            });
        } else if ("employer".equalsIgnoreCase(request.getRole())) {
            employerRepository.findById(request.getUserId()).ifPresent(employer -> {
                employer.setAccountBannedStatus(true);
                employer.setBannedReason(request.getReason());
                employer.setUnbanDate(request.getEndDate());
                employerRepository.save(employer);
            });
        } else {
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }
    }

    public void unsuspendUser(UnsuspendUserRequest request) {
        if ("freelancer".equalsIgnoreCase(request.getRole())) {
            freelancerRepository.findById(request.getUserId()).ifPresent(freelancer -> {
                freelancer.setAccountBannedStatus(false);
                freelancer.setBannedReason(null);
                freelancer.setUnbanDate(null);
                freelancerRepository.save(freelancer);
            });
        } else if ("employer".equalsIgnoreCase(request.getRole())) {
            employerRepository.findById(request.getUserId()).ifPresent(employer -> {
                employer.setAccountBannedStatus(false);
                employer.setBannedReason(null);
                employer.setUnbanDate(null);
                employerRepository.save(employer);
            });
        } else {
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }
    }
}
