package com.giguniverse.backend.Profile.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Feedback.Repository.FreelancerFeedbackRepository;
import com.giguniverse.backend.Feedback.Service.FreelancerFeedbackService;
import com.giguniverse.backend.JobPost.ContractHandling.Model.Contract;
import com.giguniverse.backend.JobPost.ContractHandling.Repository.ContractRepository;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;
import com.giguniverse.backend.JobPost.CreateJobPost.repository.JobPostRepository;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Model.FreelancerProfile;
import com.giguniverse.backend.Profile.Model.DTO.FreelancerProfileDataResponse;
import com.giguniverse.backend.Profile.Model.DTO.FreelancerProfileFormData;
import com.giguniverse.backend.Profile.Model.DTO.JobHistoryRecordDTO;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerCertification;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerEducation;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerEducation.EducationItem;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerJobExperience;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerJobExperience.JobExperienceItem;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerPortfolio;
import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerResume;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;
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

    @Autowired
    private FreelancerFeedbackService freelancerFeedbackService;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private FreelancerFeedbackRepository freelancerFeedbackRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

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
        FreelancerProfile pgProfile = pgProfileRepo.findByFreelancer(freelancer)
            .orElseGet(() -> {
                FreelancerProfile newProfile = new FreelancerProfile();
                newProfile.setFreelancer(freelancer);
                freelancer.setProfile(newProfile);
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
        
        pgProfile.setSelfDescription(formData.getSelfDescription());
        pgProfile.setHighestEducationLevel(formData.getHighestEducationLevel());
        pgProfile.setHoursPerWeek(formData.getHoursPerWeek());
        pgProfile.setJobCategory(String.join(",", formData.getJobCategory()));
        pgProfile.setPreferredJobTitle(String.join(",", formData.getPreferredJobTitle()));
        pgProfile.setSkillTags(String.join(",", formData.getSkillTags()));
        pgProfile.setLanguageProficiency(mapper.writeValueAsString(formData.getLanguageProficiency()));
        pgProfile.setPreferredPayrate(Integer.parseInt(formData.getPreferredPayRate()));
        pgProfile.setOpenToWork(formData.isOpenToWork());

        if (pgProfile.getPremiumStatus() == null) {
            pgProfile.setPremiumStatus(false);
        }
        
        pgProfileRepo.save(pgProfile);

        // 2. Save to MongoDB collections
        // Resume
        if (resumeFile != null) {

            List<FreelancerResume> existingResumes = mongoResumeRepo.findByUserId(userId);
            if (!existingResumes.isEmpty()) {
                mongoResumeRepo.deleteByUserId(userId);
            }

            FreelancerResume resume = new FreelancerResume();
            resume.setUserId(userId);
            resume.setFileName(resumeFile.getOriginalFilename());
            resume.setFileData(resumeFile.getBytes());
            resume.setContentType(resumeFile.getContentType());
            mongoResumeRepo.save(resume);
        }

        // Job Experiences
        if (formData.getJobExperiences() != null && !formData.getJobExperiences().isEmpty()) {

            List<FreelancerJobExperience> existingJobExps = mongoJobExpRepo.findByUserId(userId);
            if (!existingJobExps.isEmpty()) {
                mongoJobExpRepo.deleteByUserId(userId);
            }

            FreelancerJobExperience jobExp = new FreelancerJobExperience();
            jobExp.setUserId(userId);
            jobExp.setJobExperiences(formData.getJobExperiences().stream()
                .map(exp -> new JobExperienceItem(exp.getJobTitle(), exp.getFromDate(), exp.getToDate(), exp.getCompany(), exp.getDescription(), exp.isCurrentJob()))
                .collect(Collectors.toList()));
            mongoJobExpRepo.save(jobExp);
        }

        // Portfolio Files
        if (portfolioFiles != null) {

            List<FreelancerPortfolio> existingPortfolios = mongoPortfolioRepo.findByUserId(userId);
            if (!existingPortfolios.isEmpty()) {
                mongoPortfolioRepo.deleteByUserId(userId);
            }

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
        if (formData.getEducations() != null && !formData.getEducations().isEmpty()) {

            List<FreelancerEducation> existingEducations = mongoEduRepo.findByUserId(userId);
            if (!existingEducations.isEmpty()) {
                mongoEduRepo.deleteByUserId(userId);
            }

            FreelancerEducation education = new FreelancerEducation();
            education.setUserId(userId);
            education.setEducationExperiences(formData.getEducations().stream()
                .map(edu -> new EducationItem(edu.getInstitute(), edu.getTitle(), edu.getCourseName(), edu.getFromDate(), edu.getToDate(), edu.isCurrentStudying()))
                .collect(Collectors.toList()));
            mongoEduRepo.save(education);
        }


        // Certifications
        if (certificationFiles != null) {

            List<FreelancerCertification> existingCerts = mongoCertRepo.findByUserId(userId);
            if (!existingCerts.isEmpty()) {
                mongoCertRepo.deleteByUserId(userId);
            }


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



    // Function to get the full freelancer profile data
    public FreelancerProfileDataResponse getFullFreelancerProfile() {
        String userId = AuthUtil.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }

        FreelancerProfile pgProfile = pgProfileRepo.findByFreelancer_FreelancerUserId(userId)
                .orElseThrow(() -> new RuntimeException("Freelancer profile not found"));

        FreelancerFeedbackService.FreelancerRatingResponse ratingData = freelancerFeedbackService.getFreelancerRating(userId);

        FreelancerProfileDataResponse response = new FreelancerProfileDataResponse();
            response.setFreelancerProfileId(userId);
            response.setFullName(pgProfile.getFullName());
            response.setUsername(pgProfile.getUsername());
            response.setGender(pgProfile.getGender());
            response.setDob(pgProfile.getDob().toString());
            response.setEmail(pgProfile.getEmail());
            response.setPhone(pgProfile.getPhone());
            response.setLocation(pgProfile.getLocation());
            response.setProfilePicture(Base64.getEncoder().encodeToString(pgProfile.getProfilePicture()));
            response.setProfilePictureMimeType(pgProfile.getProfilePictureMimeType());
            response.setSelfDescription(pgProfile.getSelfDescription());
            response.setHighestEducationLevel(pgProfile.getHighestEducationLevel());
            response.setHoursPerWeek(pgProfile.getHoursPerWeek());
            response.setJobCategory(pgProfile.getJobCategory());
            response.setPreferredJobTitle(pgProfile.getPreferredJobTitle());
            response.setSkillTags(pgProfile.getSkillTags());
            response.setLanguageProficiency(pgProfile.getLanguageProficiency());
            response.setPreferredPayRate(pgProfile.getPreferredPayrate());
            response.setOpenToWork(pgProfile.getOpenToWork());
            response.setPremiumStatus(pgProfile.getPremiumStatus());
            response.setAverageRating(ratingData.averageRating());
            response.setTotalRatings(ratingData.totalRatings());

        // MongoDB section

        // 1. Resume
        List<FreelancerResume> resumes = mongoResumeRepo.findByUserId(userId);
        if (!resumes.isEmpty()) {
            FreelancerResume resume = resumes.get(0);
            FreelancerProfileDataResponse.Resume resumeDto = new FreelancerProfileDataResponse.Resume();
            resumeDto.setFileName(resume.getFileName());
            resumeDto.setContentType(resume.getContentType());
            resumeDto.setBase64Data(Base64.getEncoder().encodeToString(resume.getFileData()));
            response.setResumeFile(resumeDto);
        }

        // 2. Portfolios
        List<FreelancerPortfolio> portfolioList = mongoPortfolioRepo.findByUserId(userId);
        if (!portfolioList.isEmpty()) {
            List<FreelancerProfileDataResponse.PortfolioFile> portfolioDtos = portfolioList.stream()
                .map(file -> {
                    FreelancerProfileDataResponse.PortfolioFile dto = new FreelancerProfileDataResponse.PortfolioFile();
                    dto.setFileName(file.getFileName());
                    dto.setContentType(file.getContentType());
                    dto.setBase64Data(Base64.getEncoder().encodeToString(file.getFileData()));
                    return dto;
                }).collect(Collectors.toList());
            response.setPortfolioFiles(portfolioDtos);
        }

        // 3. Certifications
        List<FreelancerCertification> certList = mongoCertRepo.findByUserId(userId);
        if (!certList.isEmpty()) {
            List<FreelancerProfileDataResponse.CertificateFile> certDtos = certList.stream()
                .map(file -> {
                    FreelancerProfileDataResponse.CertificateFile dto = new FreelancerProfileDataResponse.CertificateFile();
                    dto.setFileName(file.getFileName());
                    dto.setContentType(file.getContentType());
                    dto.setBase64Data(Base64.getEncoder().encodeToString(file.getFileData()));
                    return dto;
                }).collect(Collectors.toList());
            response.setCertificationFiles(certDtos);
        }

        // 4. Job Experiences
        List<FreelancerJobExperience> jobExpList = mongoJobExpRepo.findByUserId(userId);
        if (!jobExpList.isEmpty()) {
            FreelancerJobExperience jobExp = jobExpList.get(0); 
            if (!jobExp.getJobExperiences().isEmpty()) {
                List<FreelancerProfileDataResponse.JobExperience> jobDtos = jobExp.getJobExperiences().stream()
                    .map(exp -> {
                        FreelancerProfileDataResponse.JobExperience dto = new FreelancerProfileDataResponse.JobExperience();
                        dto.setJobTitle(exp.getJobTitle());
                        dto.setFromDate(exp.getFromDate());
                        dto.setToDate(exp.getToDate());
                        dto.setCompany(exp.getCompany());
                        dto.setDescription(exp.getDescription());
                        dto.setCurrentJob(exp.isCurrentJob());
                        return dto;
                    }).collect(Collectors.toList());
                response.setJobExperiences(jobDtos);
            }
        }

        // 5. Education
        List<FreelancerEducation> eduList = mongoEduRepo.findByUserId(userId);
        if (!eduList.isEmpty()) {
            FreelancerEducation edu = eduList.get(0);
            if (!edu.getEducationExperiences().isEmpty()) {
                List<FreelancerProfileDataResponse.Education> eduDtos = edu.getEducationExperiences().stream()
                    .map(item -> {
                        FreelancerProfileDataResponse.Education dto = new FreelancerProfileDataResponse.Education();
                        dto.setInstitute(item.getInstitute());
                        dto.setTitle(item.getTitle());
                        dto.setCourseName(item.getCourseName());
                        dto.setFromDate(item.getFromDate());
                        dto.setToDate(item.getToDate());
                        dto.setCurrentStudying(item.isCurrentStudying());
                        return dto;
                    }).collect(Collectors.toList());
                response.setEducations(eduDtos);
            }
        }

        return response;
    }

    
    public List<JobHistoryRecordDTO> getFreelancerJobHistory() {
        String freelancerId = AuthUtil.getUserId();
        if (freelancerId == null) throw new RuntimeException("User not authenticated");

        List<Contract> contracts = contractRepository.findByFreelancerId(freelancerId);

        return contracts.stream().map(contract -> {

            // Fetch JobPost
            JobPost job = jobPostRepository.findById(Integer.parseInt(contract.getJobId()))
                    .orElse(null);

            // Fetch Employer Name
            String employerName = employerProfileRepository
                    .findByEmployer_EmployerUserId(contract.getEmployerId())
                    .map(EmployerProfile::getFullName)
                    .orElse("N/A");

            // Fetch Feedback
            var feedbackList = freelancerFeedbackRepository
                    .findByFreelancerIdAndContractId(freelancerId, contract.getContractId());
            Double rating = feedbackList.isEmpty() ? null : Double.valueOf(feedbackList.get(0).getRating());
            String feedbackText = feedbackList.isEmpty() ? null : feedbackList.get(0).getFeedback();

            return new JobHistoryRecordDTO(
                    String.valueOf(contract.getContractId()),
                    job != null ? job.getJobTitle() : "N/A",
                    employerName,
                    contract.getEmployerId(),
                    job != null ? job.getCompanyName() : null,
                    contract.getContractStatus(),
                    contract.getContractStartDate().toString(),
                    contract.getContractEndDate() != null ? contract.getContractEndDate().toString() : null,
                    contract.getAgreedPayRatePerHour(),
                    rating != null ? rating : 0,
                    feedbackText,
                    job != null && job.getSkillTags() != null ? List.of(job.getSkillTags().split(",")) : List.of(),
                    contract.getCancellationReason()
            );
        }).collect(Collectors.toList());
    }



    public FreelancerProfileDataResponse getViewFreelancerProfile(String userId) {
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }

        FreelancerProfile pgProfile = pgProfileRepo.findByFreelancer_FreelancerUserId(userId)
                .orElseThrow(() -> new RuntimeException("Freelancer profile not found"));

        FreelancerFeedbackService.FreelancerRatingResponse ratingData = freelancerFeedbackService.getFreelancerRating(userId);

        FreelancerProfileDataResponse response = new FreelancerProfileDataResponse();
            response.setFreelancerProfileId(userId);
            response.setFullName(pgProfile.getFullName());
            response.setUsername(pgProfile.getUsername());
            response.setGender(pgProfile.getGender());
            response.setDob(pgProfile.getDob().toString());
            response.setEmail(pgProfile.getEmail());
            response.setPhone(pgProfile.getPhone());
            response.setLocation(pgProfile.getLocation());
            response.setProfilePicture(Base64.getEncoder().encodeToString(pgProfile.getProfilePicture()));
            response.setProfilePictureMimeType(pgProfile.getProfilePictureMimeType());
            response.setSelfDescription(pgProfile.getSelfDescription());
            response.setHighestEducationLevel(pgProfile.getHighestEducationLevel());
            response.setHoursPerWeek(pgProfile.getHoursPerWeek());
            response.setJobCategory(pgProfile.getJobCategory());
            response.setPreferredJobTitle(pgProfile.getPreferredJobTitle());
            response.setSkillTags(pgProfile.getSkillTags());
            response.setLanguageProficiency(pgProfile.getLanguageProficiency());
            response.setPreferredPayRate(pgProfile.getPreferredPayrate());
            response.setOpenToWork(pgProfile.getOpenToWork());
            response.setPremiumStatus(pgProfile.getPremiumStatus());
            response.setAverageRating(ratingData.averageRating());
            response.setTotalRatings(ratingData.totalRatings());

        // MongoDB section

        // 1. Resume
        List<FreelancerResume> resumes = mongoResumeRepo.findByUserId(userId);
        if (!resumes.isEmpty()) {
            FreelancerResume resume = resumes.get(0);
            FreelancerProfileDataResponse.Resume resumeDto = new FreelancerProfileDataResponse.Resume();
            resumeDto.setFileName(resume.getFileName());
            resumeDto.setContentType(resume.getContentType());
            resumeDto.setBase64Data(Base64.getEncoder().encodeToString(resume.getFileData()));
            response.setResumeFile(resumeDto);
        }

        // 2. Portfolios
        List<FreelancerPortfolio> portfolioList = mongoPortfolioRepo.findByUserId(userId);
        if (!portfolioList.isEmpty()) {
            List<FreelancerProfileDataResponse.PortfolioFile> portfolioDtos = portfolioList.stream()
                .map(file -> {
                    FreelancerProfileDataResponse.PortfolioFile dto = new FreelancerProfileDataResponse.PortfolioFile();
                    dto.setFileName(file.getFileName());
                    dto.setContentType(file.getContentType());
                    dto.setBase64Data(Base64.getEncoder().encodeToString(file.getFileData()));
                    return dto;
                }).collect(Collectors.toList());
            response.setPortfolioFiles(portfolioDtos);
        }

        // 3. Certifications
        List<FreelancerCertification> certList = mongoCertRepo.findByUserId(userId);
        if (!certList.isEmpty()) {
            List<FreelancerProfileDataResponse.CertificateFile> certDtos = certList.stream()
                .map(file -> {
                    FreelancerProfileDataResponse.CertificateFile dto = new FreelancerProfileDataResponse.CertificateFile();
                    dto.setFileName(file.getFileName());
                    dto.setContentType(file.getContentType());
                    dto.setBase64Data(Base64.getEncoder().encodeToString(file.getFileData()));
                    return dto;
                }).collect(Collectors.toList());
            response.setCertificationFiles(certDtos);
        }

        // 4. Job Experiences
        List<FreelancerJobExperience> jobExpList = mongoJobExpRepo.findByUserId(userId);
        if (!jobExpList.isEmpty()) {
            FreelancerJobExperience jobExp = jobExpList.get(0); 
            if (!jobExp.getJobExperiences().isEmpty()) {
                List<FreelancerProfileDataResponse.JobExperience> jobDtos = jobExp.getJobExperiences().stream()
                    .map(exp -> {
                        FreelancerProfileDataResponse.JobExperience dto = new FreelancerProfileDataResponse.JobExperience();
                        dto.setJobTitle(exp.getJobTitle());
                        dto.setFromDate(exp.getFromDate());
                        dto.setToDate(exp.getToDate());
                        dto.setCompany(exp.getCompany());
                        dto.setDescription(exp.getDescription());
                        dto.setCurrentJob(exp.isCurrentJob());
                        return dto;
                    }).collect(Collectors.toList());
                response.setJobExperiences(jobDtos);
            }
        }

        // 5. Education
        List<FreelancerEducation> eduList = mongoEduRepo.findByUserId(userId);
        if (!eduList.isEmpty()) {
            FreelancerEducation edu = eduList.get(0);
            if (!edu.getEducationExperiences().isEmpty()) {
                List<FreelancerProfileDataResponse.Education> eduDtos = edu.getEducationExperiences().stream()
                    .map(item -> {
                        FreelancerProfileDataResponse.Education dto = new FreelancerProfileDataResponse.Education();
                        dto.setInstitute(item.getInstitute());
                        dto.setTitle(item.getTitle());
                        dto.setCourseName(item.getCourseName());
                        dto.setFromDate(item.getFromDate());
                        dto.setToDate(item.getToDate());
                        dto.setCurrentStudying(item.isCurrentStudying());
                        return dto;
                    }).collect(Collectors.toList());
                response.setEducations(eduDtos);
            }
        }

        return response;
    }


    public List<JobHistoryRecordDTO> getViewFreelancerJobHistory(String userId) {

        if (userId == null) throw new RuntimeException("User not authenticated");

        List<Contract> contracts = contractRepository.findByFreelancerId(userId);

        return contracts.stream().map(contract -> {

            // Fetch JobPost
            JobPost job = jobPostRepository.findById(Integer.parseInt(contract.getJobId()))
                    .orElse(null);

            // Fetch Employer Name
            String employerName = employerProfileRepository
                    .findByEmployer_EmployerUserId(contract.getEmployerId())
                    .map(EmployerProfile::getFullName)
                    .orElse("N/A");

            // Fetch Feedback
            var feedbackList = freelancerFeedbackRepository
                    .findByFreelancerIdAndContractId(userId, contract.getContractId());
            Double rating = feedbackList.isEmpty() ? null : Double.valueOf(feedbackList.get(0).getRating());
            String feedbackText = feedbackList.isEmpty() ? null : feedbackList.get(0).getFeedback();

            return new JobHistoryRecordDTO(
                    String.valueOf(contract.getContractId()),
                    job != null ? job.getJobTitle() : "N/A",
                    employerName,
                    contract.getEmployerId(),
                    job != null ? job.getCompanyName() : null,
                    contract.getContractStatus(),
                    contract.getContractStartDate().toString(),
                    contract.getContractEndDate() != null ? contract.getContractEndDate().toString() : null,
                    contract.getAgreedPayRatePerHour(),
                    rating != null ? rating : 0,
                    feedbackText,
                    job != null && job.getSkillTags() != null ? List.of(job.getSkillTags().split(",")) : List.of(),
                    contract.getCancellationReason()
            );
        }).collect(Collectors.toList());
    }
}
