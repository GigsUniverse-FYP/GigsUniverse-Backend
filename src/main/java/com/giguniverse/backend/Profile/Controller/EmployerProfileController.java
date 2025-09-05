package com.giguniverse.backend.Profile.Controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giguniverse.backend.Auth.Session.AuthUtil;

import com.giguniverse.backend.Profile.Model.DTO.EmployerProfileDataResponse;
import com.giguniverse.backend.Profile.Model.DTO.EmployerProfileFormData;
import com.giguniverse.backend.Profile.Model.DTO.FreelancerFeedbackDTO;
import com.giguniverse.backend.Profile.Service.EmployerProfileService;

@CrossOrigin
@RestController
@RequestMapping("/api/profile/employer")
public class EmployerProfileController {
    private static final Logger logger = LoggerFactory.getLogger(EmployerProfileController.class);

    @Autowired
    private EmployerProfileService employerProfileService;

    @GetMapping("/session")
    public ResponseEntity<?> getCurrentUserInfo() {
        return employerProfileService.getCurrentUser();
    }

    public EmployerProfileController(EmployerProfileService employerProfileService) {
        this.employerProfileService = employerProfileService;
    }

    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProfile(
            @RequestPart("data") String data,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestPart(value = "certificationFiles", required = false) List<MultipartFile> certificationFiles) {
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            logger.debug("Received data: {}", data);
            EmployerProfileFormData formData = mapper.readValue(data, EmployerProfileFormData.class);
            String userId = AuthUtil.getUserId();
            String email = AuthUtil.getUserEmail();

            employerProfileService.saveProfile(
                formData, userId, email, 
                profilePicture, certificationFiles
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
        EmployerProfileDataResponse response = employerProfileService.getFullEmployerProfile();
        return ResponseEntity.ok(response);
    }

   @GetMapping("/freelancer-feedback")
    public ResponseEntity<List<FreelancerFeedbackDTO>> getFreelancerFeedback() {
        List<FreelancerFeedbackDTO> feedbackList = employerProfileService.getFreelancerFeedbackForEmployer();
        return ResponseEntity.ok(feedbackList);
    }

    @GetMapping("/view-employer-profile/{userId}")
    public ResponseEntity<EmployerProfileDataResponse> getViewEmployerProfile(@PathVariable String userId) {
        EmployerProfileDataResponse response = employerProfileService.getViewFullEmployerProfile(userId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/view-employer-feedback/{userId}")
    public List<FreelancerFeedbackDTO> getViewEmployerFeedback(@PathVariable String userId) {
        return employerProfileService.getViewFreelancerFeedbackForEmployer(userId);
    }

}
