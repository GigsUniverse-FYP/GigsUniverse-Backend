package com.giguniverse.backend.Profile.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
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
import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Feedback.Model.EmployerFeedback;
import com.giguniverse.backend.Feedback.Repository.EmployerFeedbackRepository;
import com.giguniverse.backend.Feedback.Service.EmployerFeedbackService;
import com.giguniverse.backend.JobPost.ContractHandling.Model.Contract;
import com.giguniverse.backend.JobPost.ContractHandling.Repository.ContractRepository;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;
import com.giguniverse.backend.JobPost.CreateJobPost.repository.JobPostRepository;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Model.FreelancerProfile;
import com.giguniverse.backend.Profile.Model.DTO.EmployerProfileDataResponse;
import com.giguniverse.backend.Profile.Model.DTO.EmployerProfileFormData;
import com.giguniverse.backend.Profile.Model.DTO.FreelancerFeedbackDTO;
import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerJobExperience;
import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerJobExperience.JobExperienceItem;
import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerCertification;
import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerEducation;
import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerEducation.EducationItem;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;
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

    @Autowired
    private EmployerFeedbackService employerFeedbackService;
    @Autowired
    private JobPostRepository jobPostRepository;
    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private EmployerFeedbackRepository employerFeedbackRepository;
    @Autowired
    private FreelancerProfileRepository freelancerProfileRepository;

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
        EmployerProfile pgProfile = pgProfileRepo.findByEmployer(employer)
            .orElseGet(() -> {
                EmployerProfile newProfile = new EmployerProfile();
                newProfile.setEmployer(employer);
                employer.setProfile(newProfile);
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
        pgProfile.setLanguageProficiency(mapper.writeValueAsString(formData.getLanguageProficiency()));
        pgProfile.setOpenToHire(formData.isOpenToHire());

        if (pgProfile.getPremiumStatus() == null) {
            pgProfile.setPremiumStatus(false);
        }

        if (pgProfile.getAvailableCredits() == null){
            pgProfile.setAvailableCredits(0L);
        }
        

        pgProfileRepo.save(pgProfile);

        if (formData.getJobExperiences() != null && !formData.getJobExperiences().isEmpty()) {

            List<EmployerJobExperience> existingJobExps = mongoJobExpRepo.findByUserId(userId);
            if (!existingJobExps.isEmpty()) {
                mongoJobExpRepo.deleteByUserId(userId);
            }


            EmployerJobExperience jobExp = new EmployerJobExperience();
            jobExp.setUserId(userId);
            jobExp.setJobExperiences(formData.getJobExperiences().stream()
                .map(exp -> new JobExperienceItem(exp.getJobTitle(), exp.getFromDate(), exp.getToDate(), exp.getCompany(), exp.getDescription(), exp.isCurrentJob()))
                .collect(Collectors.toList()));
            mongoJobExpRepo.save(jobExp);
        }

        // Education
        if (formData.getEducations() != null && !formData.getEducations().isEmpty()) {

            
            List<EmployerEducation> existingEducations = mongoEduRepo.findByUserId(userId);
            if (!existingEducations.isEmpty()) {
                mongoEduRepo.deleteByUserId(userId);
            }

            EmployerEducation education = new EmployerEducation();
            education.setUserId(userId);
            education.setEducationExperiences(formData.getEducations().stream()
                .map(edu -> new EducationItem(edu.getInstitute(), edu.getTitle(), edu.getCourseName(), edu.getFromDate(), edu.getToDate(), edu.isCurrentStudying()))
                .collect(Collectors.toList()));
            mongoEduRepo.save(education);
        }

        // Certifications
        if (certificationFiles != null) {

            List<EmployerCertification> existingCerts = mongoCertRepo.findByUserId(userId);
            if (!existingCerts.isEmpty()) {
                mongoCertRepo.deleteByUserId(userId);
            }

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

    public EmployerProfileDataResponse getFullEmployerProfile() {
        String userId = AuthUtil.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }

        EmployerProfile pgProfile = pgProfileRepo.findByEmployer_EmployerUserId(userId)
                .orElseThrow(() -> new RuntimeException("Employer profile not found"));


        EmployerFeedbackService.EmployerRatingResponse ratingData = employerFeedbackService.getEmployerRating(userId);


        EmployerProfileDataResponse response = new EmployerProfileDataResponse();
            response.setEmployerProfileId(userId);
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
            response.setLanguageProficiency(pgProfile.getLanguageProficiency());
            response.setOpenToHire(pgProfile.getOpenToHire());
            response.setPremiumStatus(pgProfile.getPremiumStatus());
            response.setAverageRating(ratingData.averageRating());
            response.setTotalRatings(ratingData.totalRatings());
        // MongoDB section

        // 1. Certifications
        List<EmployerCertification> certList = mongoCertRepo.findByUserId(userId);
        if (!certList.isEmpty()) {
            List<EmployerProfileDataResponse.CertificateFile> certDtos = certList.stream()
                .map(file -> {
                    EmployerProfileDataResponse.CertificateFile dto = new EmployerProfileDataResponse.CertificateFile();
                    dto.setFileName(file.getFileName());
                    dto.setContentType(file.getContentType());
                    dto.setBase64Data(Base64.getEncoder().encodeToString(file.getFileData()));
                    return dto;
                }).collect(Collectors.toList());
            response.setCertificationFiles(certDtos);
        }

        // 2. Job Experiences
        List<EmployerJobExperience> jobExpList = mongoJobExpRepo.findByUserId(userId);
        if (!jobExpList.isEmpty()) {
            EmployerJobExperience jobExp = jobExpList.get(0);
            if (!jobExp.getJobExperiences().isEmpty()) {
                List<EmployerProfileDataResponse.JobExperience> jobDtos = jobExp.getJobExperiences().stream()
                    .map(exp -> {
                        EmployerProfileDataResponse.JobExperience dto = new EmployerProfileDataResponse.JobExperience();
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

        // 3. Education
        List<EmployerEducation> eduList = mongoEduRepo.findByUserId(userId);
        if (!eduList.isEmpty()) {
            EmployerEducation edu = eduList.get(0);
            if (!edu.getEducationExperiences().isEmpty()) {
                List<EmployerProfileDataResponse.Education> eduDtos = edu.getEducationExperiences().stream()
                    .map(item -> {
                        EmployerProfileDataResponse.Education dto = new EmployerProfileDataResponse.Education();
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

    public List<FreelancerFeedbackDTO> getFreelancerFeedbackForEmployer() {
        String employerId = AuthUtil.getUserId();
        if (employerId == null) throw new RuntimeException("User not authenticated");

        List<EmployerFeedback> feedbackList = employerFeedbackRepository.findByEmployerId(employerId);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

        return feedbackList.stream().map(feedback -> {
            Contract contract = contractRepository.findById(feedback.getContractId())
                    .orElseThrow(() -> new RuntimeException("Contract not found"));

            JobPost job = jobPostRepository.findById(feedback.getJobId())
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            FreelancerProfile freelancer = freelancerProfileRepository
                    .findByFreelancer_FreelancerUserId(feedback.getFreelancerId())
                    .orElseThrow(() -> new RuntimeException("Freelancer not found"));

            return new FreelancerFeedbackDTO(
                    freelancer.getFreelancer().getFreelancerUserId(),
                    freelancer.getFullName(),
                    feedback.getRating(),
                    feedback.getFeedback(),
                    contract.getAgreedPayRatePerHour(),
                    contract.getContractStartDate() != null ? sdf.format(contract.getContractStartDate()) : null,
                    contract.getContractEndDate() != null ? sdf.format(contract.getContractEndDate()) : null,
                    String.valueOf(job.getJobPostId()),
                    job.getJobTitle()
            );
        }).collect(Collectors.toList());
    }


    public EmployerProfileDataResponse getViewFullEmployerProfile(String userId) {
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }

        EmployerProfile pgProfile = pgProfileRepo.findByEmployer_EmployerUserId(userId)
                .orElseThrow(() -> new RuntimeException("Employer profile not found"));


        EmployerFeedbackService.EmployerRatingResponse ratingData = employerFeedbackService.getEmployerRating(userId);


        EmployerProfileDataResponse response = new EmployerProfileDataResponse();
            response.setEmployerProfileId(userId);
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
            response.setLanguageProficiency(pgProfile.getLanguageProficiency());
            response.setOpenToHire(pgProfile.getOpenToHire());
            response.setPremiumStatus(pgProfile.getPremiumStatus());
            response.setAverageRating(ratingData.averageRating());
            response.setTotalRatings(ratingData.totalRatings());
        // MongoDB section

        // 1. Certifications
        List<EmployerCertification> certList = mongoCertRepo.findByUserId(userId);
        if (!certList.isEmpty()) {
            List<EmployerProfileDataResponse.CertificateFile> certDtos = certList.stream()
                .map(file -> {
                    EmployerProfileDataResponse.CertificateFile dto = new EmployerProfileDataResponse.CertificateFile();
                    dto.setFileName(file.getFileName());
                    dto.setContentType(file.getContentType());
                    dto.setBase64Data(Base64.getEncoder().encodeToString(file.getFileData()));
                    return dto;
                }).collect(Collectors.toList());
            response.setCertificationFiles(certDtos);
        }

        // 2. Job Experiences
        List<EmployerJobExperience> jobExpList = mongoJobExpRepo.findByUserId(userId);
        if (!jobExpList.isEmpty()) {
            EmployerJobExperience jobExp = jobExpList.get(0);
            if (!jobExp.getJobExperiences().isEmpty()) {
                List<EmployerProfileDataResponse.JobExperience> jobDtos = jobExp.getJobExperiences().stream()
                    .map(exp -> {
                        EmployerProfileDataResponse.JobExperience dto = new EmployerProfileDataResponse.JobExperience();
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

        // 3. Education
        List<EmployerEducation> eduList = mongoEduRepo.findByUserId(userId);
        if (!eduList.isEmpty()) {
            EmployerEducation edu = eduList.get(0);
            if (!edu.getEducationExperiences().isEmpty()) {
                List<EmployerProfileDataResponse.Education> eduDtos = edu.getEducationExperiences().stream()
                    .map(item -> {
                        EmployerProfileDataResponse.Education dto = new EmployerProfileDataResponse.Education();
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

    public List<FreelancerFeedbackDTO> getViewFreelancerFeedbackForEmployer(String userId) {
        if (userId == null) throw new RuntimeException("User not authenticated");

        List<EmployerFeedback> feedbackList = employerFeedbackRepository.findByEmployerId(userId);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

        return feedbackList.stream().map(feedback -> {
            Contract contract = contractRepository.findById(feedback.getContractId())
                    .orElseThrow(() -> new RuntimeException("Contract not found"));

            JobPost job = jobPostRepository.findById(feedback.getJobId())
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            FreelancerProfile freelancer = freelancerProfileRepository
                    .findByFreelancer_FreelancerUserId(feedback.getFreelancerId())
                    .orElseThrow(() -> new RuntimeException("Freelancer not found"));

            return new FreelancerFeedbackDTO(
                    freelancer.getFreelancer().getFreelancerUserId(),
                    freelancer.getFullName(),
                    feedback.getRating(),
                    feedback.getFeedback(),
                    contract.getAgreedPayRatePerHour(),
                    contract.getContractStartDate() != null ? sdf.format(contract.getContractStartDate()) : null,
                    contract.getContractEndDate() != null ? sdf.format(contract.getContractEndDate()) : null,
                    String.valueOf(job.getJobPostId()),
                    job.getJobTitle()
            );
        }).collect(Collectors.toList());
    }


}
