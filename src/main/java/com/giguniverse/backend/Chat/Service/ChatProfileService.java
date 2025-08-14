package com.giguniverse.backend.Chat.Service;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.Repository.AdminRepository;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Chat.Model.ChatUserInfoDTO;

@Service
public class ChatProfileService {
    @Autowired
    private FreelancerRepository freelancerRepo;
    @Autowired
    private EmployerRepository employerRepo;
    @Autowired
    private AdminRepository adminRepo;


    public ChatUserInfoDTO getUserInfoById(String userId) {
        // Try Freelancer
        return freelancerRepo.findById(userId).map(f -> new ChatUserInfoDTO(
            f.getFreelancerUserId(),
            f.getProfile().getFullName(),
            f.getProfile().getUsername(),  
            f.getRole(),
            avatarToDataUri(f.getProfile().getProfilePicture(), f.getProfile().getProfilePictureMimeType()),
            f.getProfile().getProfilePictureMimeType(),
            f.isOnlineStatus()
        )).orElseGet(() ->
            // Try Employer
            employerRepo.findById(userId).map(e -> new ChatUserInfoDTO(
                e.getEmployerUserId(),
                e.getProfile().getFullName(),
                e.getProfile().getUsername(), 
                e.getRole(),
                avatarToDataUri(e.getProfile().getProfilePicture(), e.getProfile().getProfilePictureMimeType()),
                e.getProfile().getProfilePictureMimeType(),
                e.isOnlineStatus()
            )).orElseGet(() ->
                // Try Admin
                adminRepo.findById(userId).map(a -> new ChatUserInfoDTO(
                    a.getAdminUserId(),
                    a.getProfile().getFullName(),
                    a.getProfile().getUsername(), 
                    a.getRole(),
                    avatarToDataUri(a.getProfile().getProfilePicture(), a.getProfile().getProfilePictureMimeType()),
                    a.getProfile().getProfilePictureMimeType(),
                    a.isOnlineStatus()
                )).orElse(new ChatUserInfoDTO(
                    userId,
                    "Unknown User",
                    "unknown", 
                    "Unknown",
                    null,
                    null,
                    false
                ))
            )
        );
    }

       private String avatarToDataUri(byte[] imageBytes, String mimeType) {
        if (imageBytes == null || mimeType == null) return null;
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
    }


}
