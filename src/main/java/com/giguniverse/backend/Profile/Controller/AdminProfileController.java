package com.giguniverse.backend.Profile.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Profile.Model.DTO.AdminProfileDataResponse;
import com.giguniverse.backend.Profile.Model.DTO.AdminProfileFormData;
import com.giguniverse.backend.Profile.Service.AdminProfileService;
import org.springframework.http.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;


@CrossOrigin
@RestController
@RequestMapping("/api/profile/admin")
public class AdminProfileController {

    private static final Logger logger = LoggerFactory.getLogger(AdminProfileController.class);

    @Autowired
    private AdminProfileService adminProfileService;

    @GetMapping("/session")
    public ResponseEntity<?> getCurrentUserInfo() {
        return adminProfileService.getCurrentUser();
    }


    public AdminProfileController(AdminProfileService adminProfileService) {
        this.adminProfileService = adminProfileService;
    }

    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProfile(
            @RequestPart("data") String data,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            logger.debug("Received data: {}", data);
            AdminProfileFormData formData = mapper.readValue(data, AdminProfileFormData.class);
            String userId = AuthUtil.getUserId();
            String email = AuthUtil.getUserEmail();

            adminProfileService.saveProfile(
                formData, userId, email, profilePicture
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
        AdminProfileDataResponse response = adminProfileService.getFullAdminProfile();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/view-profile/{userId}")
    public ResponseEntity<?> getViewProfile(@PathVariable String userId) {
        AdminProfileDataResponse response = adminProfileService.getViewFullAdminProfile(userId);
        return ResponseEntity.ok(response);
    }
    

}

