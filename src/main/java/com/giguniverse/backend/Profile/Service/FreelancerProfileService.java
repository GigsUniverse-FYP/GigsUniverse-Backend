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

import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Profile.Model.FreelancerProfile;
import com.giguniverse.backend.Profile.Model.DTO.FreelancerProfileFormData;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerCertification;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerEducation;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerEducation.EducationItem;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerJobExperience;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerJobExperience.JobExperienceItem;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerPortfolio;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerResume;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;
import com.giguniverse.backend.Profile.Repository.Mongo_Freelancer.FreelancerCertificationRepository;
import com.giguniverse.backend.Profile.Repository.Mongo_Freelancer.FreelancerEducationRepository;
import com.giguniverse.backend.Profile.Repository.Mongo_Freelancer.FreelancerJobExperienceRepository;
import com.giguniverse.backend.Profile.Repository.Mongo_Freelancer.FreelancerPortfolioRepository;
import com.giguniverse.backend.Profile.Repository.Mongo_Freelancer.FreelancerResumeRepository;

import jakarta.transaction.Transactional;

@Service
public class FreelancerProfileService {
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


    private final FreelancerRepository freelancerRepo;

    private final FreelancerProfileRepository pgProfileRepo;
    private final FreelancerResumeRepository mongoResumeRepo;
    private final FreelancerJobExperienceRepository mongoJobExpRepo;
    private final FreelancerEducationRepository mongoEduRepo;
    private final FreelancerCertificationRepository mongoCertRepo;
    private final FreelancerPortfolioRepository mongoPortfolioRepo;

    public FreelancerProfileService(
        FreelancerProfileRepository pgProfileRepo, 
        FreelancerResumeRepository mongoResumeRepo,
        FreelancerJobExperienceRepository mongoJobExpRepo,
        FreelancerEducationRepository mongoEduRepo,
        FreelancerCertificationRepository mongoCertRepo,
        FreelancerPortfolioRepository mongoPortfolioRepo,
        FreelancerRepository freelancerRepo
    ) {
        this.pgProfileRepo = pgProfileRepo;
        this.mongoResumeRepo = mongoResumeRepo;
        this.mongoJobExpRepo = mongoJobExpRepo;
        this.mongoEduRepo = mongoEduRepo;
        this.mongoCertRepo = mongoCertRepo;
        this.mongoPortfolioRepo = mongoPortfolioRepo;
        this.freelancerRepo = freelancerRepo;
    }

    @Transactional
    public void saveProfile(FreelancerProfileFormData formData, String userId, String email,
                           MultipartFile profilePicture,
                           MultipartFile resumeFile,
                           List<MultipartFile> portfolioFiles,
                           List<MultipartFile> certificationFiles) throws IOException {


        Freelancer freelancer = freelancerRepo.findByFreelancerUserId(userId)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));
    
        // 1. Save to PostgreSQL
        FreelancerProfile pgProfile = new FreelancerProfile();

        pgProfile.setFreelancer(freelancer);
        freelancer.setProfile(pgProfile);   

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
            pgProfile.setProfilePicture(new byte[0]); // Empty byte array
        }
        
        pgProfile.setSelfDescription(formData.getSelfDescription());
        pgProfile.setHighestEducationLevel(formData.getHighestEducationLevel());
        pgProfile.setHoursPerWeek(formData.getHoursPerWeek());
        pgProfile.setJobCategory(String.join(",", formData.getJobCategory()));
        pgProfile.setPreferredJobTitle(String.join(",", formData.getPreferredJobTitles()));
        pgProfile.setSkillTags(String.join(",", formData.getSkillTags()));
        pgProfile.setLanguageProficiency(mapper.writeValueAsString(formData.getLanguageProficiency()));
        pgProfile.setPreferredPayrate(Integer.parseInt(formData.getPreferredPayRate()));
        pgProfile.setOpenToWork(formData.isOpenToWork());
        
        pgProfileRepo.save(pgProfile);

        // 2. Save to MongoDB collections
        // Resume
        if (resumeFile != null) {
            FreelancerResume resume = new FreelancerResume();
            resume.setUserId(userId);
            resume.setFileName(resumeFile.getOriginalFilename());
            resume.setFileData(resumeFile.getBytes());
            resume.setContentType(resumeFile.getContentType());
            mongoResumeRepo.save(resume);
        }

        // Job Experiences
        if (!formData.getJobExperiences().isEmpty()) {
            FreelancerJobExperience jobExp = new FreelancerJobExperience();
            jobExp.setUserId(userId);
            jobExp.setJobExperiences(formData.getJobExperiences().stream()
                .map(exp -> new JobExperienceItem(exp.getJobTitle(), exp.getFromDate(), exp.getToDate(), exp.getCompany(), exp.getDescription(), exp.isCurrentJob()))
                .collect(Collectors.toList()));
            mongoJobExpRepo.save(jobExp);
        }

        // Portfolio Files
        if (portfolioFiles != null) {
            for (MultipartFile file : portfolioFiles) {
                FreelancerPortfolio portfolio = new FreelancerPortfolio();
                portfolio.setUserId(userId);
                portfolio.setFileName(file.getOriginalFilename());
                portfolio.setFileData(file.getBytes());
                portfolio.setContentType(file.getContentType());
                mongoPortfolioRepo.save(portfolio);
            }
        }

        // Education
        if (!formData.getEducations().isEmpty()) {
            FreelancerEducation education = new FreelancerEducation();
            education.setUserId(userId);
            education.setEducationExperiences(formData.getEducations().stream()
                .map(edu -> new EducationItem(edu.getInstitute(), edu.getTitle(), edu.getCourseName(), edu.getFromDate(), edu.getToDate(), edu.isCurrentStudying()))
                .collect(Collectors.toList()));
            mongoEduRepo.save(education);
        }


        // Certifications
        if (certificationFiles != null) {
            for (MultipartFile file : certificationFiles) {
                FreelancerCertification certification = new FreelancerCertification();
                certification.setUserId(userId);
                certification.setFileName(file.getOriginalFilename());
                certification.setFileData(file.getBytes());
                certification.setContentType(file.getContentType());
                mongoCertRepo.save(certification);
            }
        }

        // Update the freelancer's profile status
        freelancer.setCompletedProfile(true);
        freelancerRepo.save(freelancer);
    }

}
