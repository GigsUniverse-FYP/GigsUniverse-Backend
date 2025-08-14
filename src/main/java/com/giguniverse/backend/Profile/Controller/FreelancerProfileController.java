package com.giguniverse.backend.Profile.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Profile.Model.DTO.FreelancerProfileDataResponse;
import com.giguniverse.backend.Profile.Model.DTO.FreelancerProfileFormData;
import com.giguniverse.backend.Profile.Service.FreelancerProfileService;
import org.springframework.http.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CrossOrigin
@RestController
@RequestMapping("/api/profile/freelancer")
public class FreelancerProfileController {

    private static final Logger logger = LoggerFactory.getLogger(FreelancerProfileController.class);

    @Autowired
    private FreelancerProfileService freelancerProfileService;

    @GetMapping("/session")
    public ResponseEntity<?> getCurrentUserInfo() {
        return freelancerProfileService.getCurrentUser();
    }


    public FreelancerProfileController(FreelancerProfileService freelancerProfileService) {
        this.freelancerProfileService = freelancerProfileService;
    }

    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProfile(
            @RequestPart("data") String data,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestPart(value = "resumeFile", required = false) MultipartFile resumeFile,
            @RequestPart(value = "portfolioFiles", required = false) List<MultipartFile> portfolioFiles,
            @RequestPart(value = "certificationFiles", required = false) List<MultipartFile> certificationFiles) {
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            logger.debug("Received data: {}", data);
            FreelancerProfileFormData formData = mapper.readValue(data, FreelancerProfileFormData.class);
            String userId = AuthUtil.getUserId();
            String email = AuthUtil.getUserEmail();

            freelancerProfileService.saveProfile(
                formData, userId, email, 
                profilePicture, resumeFile, 
                portfolioFiles, certificationFiles
            );

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error processing profile save request", e);
            logger.error("Received data: {}", data);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/my-profile")
    public ResponseEntity<?> getMyProfile() {
        FreelancerProfileDataResponse response = freelancerProfileService.getFullFreelancerProfile();
        return ResponseEntity.ok(response);
    }

}

