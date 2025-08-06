package com.giguniverse.backend.Profile.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Model.DTO.EmployerProfileFormData;
import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerJobExperience;
import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerJobExperience.JobExperienceItem;
import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerCertification;
import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerEducation;
import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerEducation.EducationItem;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;

import com.giguniverse.backend.Profile.Repository.Mongo_Employer.EmployerCertificationRepository;
import com.giguniverse.backend.Profile.Repository.Mongo_Employer.EmployerEducationRepository;
import com.giguniverse.backend.Profile.Repository.Mongo_Employer.EmployerJobExperienceRepository;

import jakarta.transaction.Transactional;


@Service
public class EmployerProfileService {
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

    private final EmployerRepository employerRepo;
    private final EmployerProfileRepository pgProfileRepo;
    
    private final EmployerJobExperienceRepository mongoJobExpRepo;
    private final EmployerEducationRepository mongoEduRepo;
    private final EmployerCertificationRepository mongoCertRepo;

    public EmployerProfileService(
        EmployerRepository employerRepo,
        EmployerProfileRepository pgProfileRepo,
        EmployerJobExperienceRepository mongoJobExpRepo,
        EmployerEducationRepository mongoEduRepo,
        EmployerCertificationRepository mongoCertRepo
    ) {
        this.employerRepo = employerRepo;
        this.pgProfileRepo = pgProfileRepo;
        this.mongoJobExpRepo = mongoJobExpRepo;
        this.mongoEduRepo = mongoEduRepo;
        this.mongoCertRepo = mongoCertRepo;
    }

    @Transactional
    public void saveProfile(EmployerProfileFormData formData, String userId, String email,
                           MultipartFile profilePicture,
                           List<MultipartFile> certificationFiles) throws IOException {

        Employer employer = employerRepo.findByEmployerUserId(userId)
                .orElseThrow(() -> new RuntimeException("Employer not found"));

        // 1. Save to PostgreSQL
        EmployerProfile pgProfile = new EmployerProfile();

        pgProfile.setEmployer(employer);
        employer.setProfile(pgProfile);

        pgProfile.setFullName(formData.getFullName());
        pgProfile.setUsername(formData.getUsername());
        pgProfile.setGender(formData.getGender());
        pgProfile.setDob(LocalDate.parse(formData.getDob()));
        pgProfile.setEmail(email);
        pgProfile.setPhone(formData.getPhone());
        pgProfile.setLocation(formData.getLocation());

        if (profilePicture != null && !profilePicture.isEmpty()) {
            pgProfile.setProfilePicture(profilePicture.getBytes());
        } else if (pgProfile.getProfilePicture() == null) {
            pgProfile.setProfilePicture(new byte[0]); 
        }

        pgProfile.setSelfDescription(formData.getSelfDescription());
        pgProfile.setLanguageProficiency(mapper.writeValueAsString(formData.getLanguageProficiency()));
        pgProfile.setOpenToHire(formData.isOpenToHire());

        pgProfileRepo.save(pgProfile);

        if (!formData.getJobExperiences().isEmpty()) {
            EmployerJobExperience jobExp = new EmployerJobExperience();
            jobExp.setUserId(userId);
            jobExp.setJobExperiences(formData.getJobExperiences().stream()
                .map(exp -> new JobExperienceItem(exp.getJobTitle(), exp.getFromDate(), exp.getToDate(), exp.getCompany(), exp.getDescription(), exp.isCurrentJob()))
                .collect(Collectors.toList()));
            mongoJobExpRepo.save(jobExp);
        }

                // Education
        if (!formData.getEducations().isEmpty()) {
            EmployerEducation education = new EmployerEducation();
            education.setUserId(userId);
            education.setEducationExperiences(formData.getEducations().stream()
                .map(edu -> new EducationItem(edu.getInstitute(), edu.getTitle(), edu.getCourseName(), edu.getFromDate(), edu.getToDate(), edu.isCurrentStudying()))
                .collect(Collectors.toList()));
            mongoEduRepo.save(education);
        }

        // Certifications
        if (certificationFiles != null) {
            for (MultipartFile file : certificationFiles) {
                EmployerCertification certification = new EmployerCertification();
                certification.setUserId(userId);
                certification.setFileName(file.getOriginalFilename());
                certification.setFileData(file.getBytes());
                certification.setContentType(file.getContentType());
                mongoCertRepo.save(certification);
            }
        }

        employer.setCompletedProfile(true);
        employerRepo.save(employer);

    }


}
