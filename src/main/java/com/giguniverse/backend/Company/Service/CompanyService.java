package com.giguniverse.backend.Company.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Company.Model.AvailableUserDTO;
import com.giguniverse.backend.Company.Model.Company;
import com.giguniverse.backend.Company.Model.CompanyAttachment;
import com.giguniverse.backend.Company.Model.CompanyImage;
import com.giguniverse.backend.Company.Model.CompanyInfoDTO;
import com.giguniverse.backend.Company.Model.CompanyResponseDTO;
import com.giguniverse.backend.Company.Model.CompanyVideo;
import com.giguniverse.backend.Company.Model.EmployeeDTO;
import com.giguniverse.backend.Company.Model.ProfileCompanyDTO;
import com.giguniverse.backend.Company.Model.UserInvolvementDTO;
import com.giguniverse.backend.Company.Model.VerifiedCompanyDTO;
import com.giguniverse.backend.Company.Repository.CompanyAttachmentRepository;
import com.giguniverse.backend.Company.Repository.CompanyImageRepository;
import com.giguniverse.backend.Company.Repository.CompanyRepository;
import com.giguniverse.backend.Company.Repository.CompanyVideoRepository;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Repository.AdminProfileRepository;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;

@Service
public class CompanyService {

        @Autowired
        private CompanyRepository companyRepository;
        @Autowired
        private CompanyAttachmentRepository companyAttachmentRepository;
        @Autowired
        private CompanyImageRepository companyImageRepository;
        @Autowired
        private CompanyVideoRepository companyVideoRepository;
        @Autowired
        private EmployerRepository employerRepository;

        @Autowired
        private EmployerProfileRepository employerProfileRepository;
        @Autowired
        private AdminProfileRepository adminProfileRepository;

        public Company createCompany(
                        String companyName, String businessRegistrationNumber, String registrationCountry,
                        Date registrationDate,
                        String industryType, String companySize, String companyDescription,
                        String registeredCompanyAddress, String businessPhoneNumber, String businessEmail,
                        String officialWebsiteUrl,
                        String taxNumber,
                        MultipartFile incorporationCertificate, MultipartFile businessLicense,
                        MultipartFile companyLogo) throws IOException {

                String userId = AuthUtil.getUserId();

                // Step 1: Save company metadata into Postgres
                Company company = Company.builder()
                                .companyName(companyName)
                                .businessRegistrationNumber(businessRegistrationNumber)
                                .registrationCountry(registrationCountry)
                                .registrationDate(registrationDate)
                                .industryType(industryType)
                                .companySize(companySize)
                                .companyDescription(companyDescription)
                                .registeredCompanyAddress(registeredCompanyAddress)
                                .businessPhoneNumber(businessPhoneNumber)
                                .businessEmail(businessEmail)
                                .creatorId(userId)
                                .companyStatus("pending")
                                .officialWebsiteUrl(officialWebsiteUrl)
                                .taxNumber(taxNumber)
                                .build();

                Company savedCompany = companyRepository.save(company);

                CompanyAttachment attachment = CompanyAttachment.builder()
                                .companyId(savedCompany.getCompanyId())
                                .companyCert(toFileData(incorporationCertificate))
                                .businessLicense(toFileData(businessLicense))
                                .companyLogo(toFileData(companyLogo))
                                .build();

                companyAttachmentRepository.save(attachment);

                return savedCompany;
        }

        private CompanyAttachment.FileData toFileData(MultipartFile file) throws IOException {
                if (file == null)
                        return null;
                return CompanyAttachment.FileData.builder()
                                .fileName(file.getOriginalFilename())
                                .fileBytes(file.getBytes())
                                .contentType(file.getContentType())
                                .build();
        }

        public List<Company> getCompaniesByCreatorId(String creatorId) {
                return companyRepository.findByCreatorId(creatorId);
        }

        public List<VerifiedCompanyDTO> getVerifiedCompanies() {
                return companyRepository.findByCompanyStatus("verified").stream()
                                .map(company -> VerifiedCompanyDTO.builder()
                                                .company(company)
                                                .attachment(companyAttachmentRepository
                                                                .findByCompanyId(company.getCompanyId()).orElse(null))
                                                .image(companyImageRepository.findByCompanyId(company.getCompanyId())
                                                                .orElse(null))
                                                .video(companyVideoRepository.findByCompanyId(company.getCompanyId())
                                                                .orElse(null))
                                                .build())
                                .collect(Collectors.toList());
        }

        public UserInvolvementDTO getUserInvolvement(String userId) {
                List<Company> verifiedCompanies = companyRepository.findByCompanyStatus("verified");

                Integer companyId = null;
                boolean isInvolved = false;

                for (Company company : verifiedCompanies) {
                        System.out.println("Checking company: " + company.getCompanyId() + " creatorId="
                                        + company.getCreatorId());
                        // Check if the user is the creator
                        if (userId.equals(company.getCreatorId())) {
                                companyId = company.getCompanyId();
                                isInvolved = true;
                                break;
                        }

                        // Check if the user is in the employerInvolved list
                        String involved = company.getEmployerInvolved();
                        if (involved != null && !involved.isEmpty()) {
                                List<String> involvedList = Arrays.stream(involved.split(","))
                                                .map(String::trim) // remove spaces
                                                .toList();
                                System.out.println("Employer involved list: " + involvedList);
                                if (involvedList.contains(userId)) {
                                        companyId = company.getCompanyId();
                                        isInvolved = true;
                                        break;
                                }
                        }
                }
                System.out.println("UserInvolvementDTO => userId=" + userId + " involved=" + isInvolved + " companyId="
                                + companyId);
                return new UserInvolvementDTO(userId, isInvolved, companyId);
        }

        public Optional<CompanyInfoDTO> getCompanyWithMediaAndEmployees(int companyId) {
                return companyRepository.findById(companyId).map(company -> {
                        CompanyAttachment attachment = companyAttachmentRepository.findByCompanyId(companyId)
                                        .orElse(null);
                        CompanyImage image = companyImageRepository.findByCompanyId(companyId).orElse(null);
                        CompanyVideo video = companyVideoRepository.findByCompanyId(companyId).orElse(null);

                        List<String> employerIds = new ArrayList<>();
                        if (company.getCreatorId() != null)
                                employerIds.add(company.getCreatorId());
                        if (company.getEmployerInvolved() != null && !company.getEmployerInvolved().isEmpty()) {
                                employerIds.addAll(Arrays.asList(company.getEmployerInvolved().split(",")));
                        }

                        List<EmployeeDTO> employees = employerIds.stream()
                                        .map(employerId -> employerRepository.findById(employerId))
                                        .filter(Optional::isPresent)
                                        .map(Optional::get)
                                        .map(employer -> {
                                                EmployerProfile profile = employer.getProfile();
                                                return EmployeeDTO.builder()
                                                                .id(employer.getEmployerUserId())
                                                                .name(profile != null ? profile.getFullName() : null)
                                                                .email(employer.getEmail())
                                                                .role(employer.getRole())
                                                                .avatar(
                                                                                profile != null && profile
                                                                                                .getProfilePicture() != null
                                                                                                                ? "data:" + profile
                                                                                                                                .getProfilePictureMimeType()
                                                                                                                                + ";base64,"
                                                                                                                                +
                                                                                                                                Base64.getEncoder()
                                                                                                                                                .encodeToString(profile
                                                                                                                                                                .getProfilePicture())
                                                                                                                : null)
                                                                .isCreator(employer.getEmployerUserId()
                                                                                .equals(company.getCreatorId()))
                                                                .build();
                                        })
                                        .toList();

                        return CompanyInfoDTO.builder()
                                        .company(company)
                                        .attachment(attachment)
                                        .image(image)
                                        .video(video)
                                        .employees(employees)
                                        .build();
                });
        }

        public List<AvailableUserDTO> getAvailableEmployers() {
                // 1. Get all companies
                List<Company> allCompanies = companyRepository.findAll();

                // 2. Collect all employer IDs already assigned (creator + involved)
                Set<String> assignedEmployerIds = new HashSet<>();

                for (Company c : allCompanies) {
                        // Add creator
                        if (c.getCreatorId() != null && !c.getCreatorId().isEmpty()) {
                                assignedEmployerIds.add(c.getCreatorId());
                        }

                        // Add involved employers
                        if (c.getEmployerInvolved() != null && !c.getEmployerInvolved().isEmpty()) {
                                String[] involvedIds = c.getEmployerInvolved().split(",");
                                for (String id : involvedIds) {
                                        String trimmedId = id.trim();
                                        if (!trimmedId.isEmpty()) {
                                                assignedEmployerIds.add(trimmedId);
                                        }
                                }
                        }
                }

                // 3. Fetch all employers who completed onboarding
                List<Employer> completedEmployers = employerRepository.findByCompletedOnboardingTrue();

                // 4. Filter out assigned employers
                List<AvailableUserDTO> availableUsers = completedEmployers.stream()
                                .filter(emp -> !assignedEmployerIds.contains(emp.getEmployerUserId()))
                                .map(emp -> new AvailableUserDTO(
                                                emp.getEmployerUserId(),
                                                emp.getProfile() != null ? emp.getProfile().getFullName() : null,
                                                emp.getProfile() != null ? emp.getProfile().getUsername() : null,
                                                emp.getRole(),
                                                emp.getProfile() != null && emp.getProfile().getProfilePicture() != null
                                                                ? "data:" + emp.getProfile().getProfilePictureMimeType()
                                                                                + ";base64,"
                                                                                + Base64.getEncoder().encodeToString(emp
                                                                                                .getProfile()
                                                                                                .getProfilePicture())
                                                                : null))
                                .toList();

                System.out.println("Assigned IDs: " + assignedEmployerIds);
                System.out.println("Available employers: " + availableUsers.size());

                return availableUsers;
        }

        @Transactional
        public void updateCompanyAndMedia(
                        Map<String, String> fields,
                        MultipartFile companyLogo,
                        List<MultipartFile> companyImages,
                        MultipartFile companyVideo,
                        boolean deleteImages,
                        boolean deleteVideo) throws IOException {
                int companyId = Integer.parseInt(fields.get("companyId"));

                // Update Postgres
                Company company = companyRepository.findById(companyId)
                                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

                company.setCompanyName(fields.get("companyName"));
                company.setBusinessRegistrationNumber(fields.get("businessRegistrationNumber"));
                company.setRegistrationCountry(fields.get("registrationCountry"));

                if (fields.get("registrationDate") != null && !fields.get("registrationDate").isBlank()) {
                        try {
                                Date parsed = new SimpleDateFormat("yyyy-MM-dd").parse(fields.get("registrationDate"));
                                company.setRegistrationDate(parsed);
                        } catch (Exception e) {
                                throw new IllegalArgumentException(
                                                "Invalid registrationDate format, expected yyyy-MM-dd");
                        }
                } else {
                        company.setRegistrationDate(null);
                }

                company.setIndustryType(fields.get("industryType"));
                company.setCompanySize(fields.get("companySize"));
                company.setCompanyDescription(fields.get("companyDescription"));
                company.setRegisteredCompanyAddress(fields.get("registeredCompanyAddress"));
                company.setBusinessPhoneNumber(fields.get("businessPhoneNumber"));
                company.setBusinessEmail(fields.get("businessEmail"));
                company.setOfficialWebsiteUrl(fields.get("officialWebsiteUrl"));
                company.setTaxNumber(fields.get("taxNumber"));
                company.setEmployerInvolved(fields.get("employerInvolved"));

                companyRepository.save(company);

                // Handle Mongo attachments
                CompanyAttachment existingAttachment = companyAttachmentRepository.findByCompanyId(companyId)
                                .orElse(null);

                if (existingAttachment == null) {
                        existingAttachment = CompanyAttachment.builder()
                                        .companyId(companyId)
                                        .build();
                }

                // Only replace logo if a new one is uploaded
                if (companyLogo != null && !companyLogo.isEmpty()) {
                        existingAttachment.setCompanyLogo(fileToData(companyLogo));
                }

                // keep existing cert & license
                companyAttachmentRepository.save(existingAttachment);

                // Reset + Save Images
                if (deleteImages) {
                        companyImageRepository.deleteByCompanyId(companyId);
                } else if (companyImages != null && !companyImages.isEmpty()) {
                        // replace
                        companyImageRepository.deleteByCompanyId(companyId);
                        List<CompanyImage.FileData> imgs = new ArrayList<>();
                        for (MultipartFile img : companyImages) {
                                if (img != null && !img.isEmpty()) {
                                        imgs.add(CompanyImage.FileData.builder()
                                                        .fileName(img.getOriginalFilename())
                                                        .contentType(img.getContentType())
                                                        .fileBytes(img.getBytes())
                                                        .build());
                                }
                        }
                        if (!imgs.isEmpty()) {
                                CompanyImage imageDoc = CompanyImage.builder()
                                                .companyId(companyId)
                                                .companyImages(imgs)
                                                .build();
                                companyImageRepository.save(imageDoc);
                        }
                }

                // Reset + Save Video
                if (deleteVideo) {
                        companyVideoRepository.deleteByCompanyId(companyId);
                } else if (companyVideo != null && !companyVideo.isEmpty()) {
                        // replace
                        companyVideoRepository.deleteByCompanyId(companyId);
                        CompanyVideo videoDoc = CompanyVideo.builder()
                                        .companyId(companyId)
                                        .companyVideo(CompanyVideo.FileData.builder()
                                                        .fileName(companyVideo.getOriginalFilename())
                                                        .contentType(companyVideo.getContentType())
                                                        .fileBytes(companyVideo.getBytes())
                                                        .build())
                                        .build();
                        companyVideoRepository.save(videoDoc);
                }
        }

        // Helper
        private CompanyAttachment.FileData fileToData(MultipartFile file) throws IOException {
                if (file == null || file.isEmpty())
                        return null;
                return CompanyAttachment.FileData.builder()
                                .fileName(file.getOriginalFilename())
                                .contentType(file.getContentType())
                                .fileBytes(file.getBytes())
                                .build();
        }

        @Transactional
        public void leaveCompany(int companyId, String userId) {
                Company company = companyRepository.findById(companyId)
                                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

                String employerInvolved = company.getEmployerInvolved();
                if (employerInvolved == null || employerInvolved.isBlank()) {
                        return; // nothing to do
                }

                // Split CSV into list
                List<String> employers = new ArrayList<>(
                                Arrays.asList(employerInvolved.split(",")));

                // Remove current user
                boolean removed = employers.removeIf(e -> e.trim().equals(userId));

                if (removed) {
                        // Rebuild CSV string
                        String updated = String.join(",", employers);
                        company.setEmployerInvolved(updated);
                        companyRepository.save(company);
                } else {
                        throw new IllegalStateException("User " + userId + " not found in employerInvolved");
                }
        }

        public boolean isUserInvolvedInAnyCompany() {
                String userId = AuthUtil.getUserId();

                if (companyRepository.existsByCreatorId(userId)) {
                        return true;
                }

                List<Company> companies = companyRepository.findAll();
                for (Company company : companies) {
                        String employerInvolved = company.getEmployerInvolved();
                        if (employerInvolved != null && !employerInvolved.isBlank()) {
                                boolean found = Arrays.stream(employerInvolved.split(","))
                                                .map(String::trim)
                                                .anyMatch(id -> id.equalsIgnoreCase(userId));
                                if (found)
                                        return true;
                        }
                }

                return false;
        }

        public ProfileCompanyDTO getUserCompany(String userId) {

                // Check if the user is a creator
                Company createdCompany = companyRepository.findByCreatorId(userId).stream().findFirst().orElse(null);
                if (createdCompany != null) {
                        return new ProfileCompanyDTO(createdCompany.getCompanyId(), createdCompany.getCompanyName(),
                                        "creator");
                }

                // Check if the user is an employer
                List<Company> allCompanies = companyRepository.findAll();
                for (Company company : allCompanies) {
                        String employerInvolved = company.getEmployerInvolved();
                        if (employerInvolved != null && !employerInvolved.isBlank()) {
                                boolean involved = Arrays.stream(employerInvolved.split(","))
                                                .map(String::trim)
                                                .anyMatch(id -> id.equalsIgnoreCase(userId));
                                if (involved) {
                                        return new ProfileCompanyDTO(company.getCompanyId(), company.getCompanyName(),
                                                        "employer");
                                }
                        }
                }

                return null;
        }

        public ProfileCompanyDTO getViewUserCompany(String userId) {
                Company createdCompany = companyRepository.findByCreatorId(userId).stream().findFirst().orElse(null);
                if (createdCompany != null) {
                        return new ProfileCompanyDTO(createdCompany.getCompanyId(), createdCompany.getCompanyName(),
                                        "creator");
                }

                List<Company> allCompanies = companyRepository.findAll();
                for (Company company : allCompanies) {
                        String employerInvolved = company.getEmployerInvolved();
                        if (employerInvolved != null && !employerInvolved.isBlank()) {
                                boolean involved = Arrays.stream(employerInvolved.split(","))
                                                .map(String::trim)
                                                .anyMatch(id -> id.equalsIgnoreCase(userId));
                                if (involved) {
                                        return new ProfileCompanyDTO(company.getCompanyId(), company.getCompanyName(),
                                                        "employer");
                                }
                        }
                }

                return null;
        }

        public VerifiedCompanyDTO getVerifiedCompanyById(String companyId) {
                return companyRepository.findById(Integer.parseInt(companyId))
                        .filter(company -> "verified".equals(company.getCompanyStatus())) // ensure it's verified
                        .map(company -> VerifiedCompanyDTO.builder()
                                .company(company)
                                .attachment(companyAttachmentRepository.findByCompanyId(company.getCompanyId()).orElse(null))
                                .image(companyImageRepository.findByCompanyId(company.getCompanyId()).orElse(null))
                                .video(companyVideoRepository.findByCompanyId(company.getCompanyId()).orElse(null))
                                .build()
                        )
                        .orElse(null); 
        }

        public List<CompanyResponseDTO> getAllCompaniesWithAttachments() {
                List<Company> companies = companyRepository.findAll();

                return companies.stream().map(company -> {
                CompanyResponseDTO.CompanyResponseDTOBuilder dto = CompanyResponseDTO.builder()
                        .companyId(company.getCompanyId())
                        .companyName(company.getCompanyName())
                        .businessRegistrationNumber(company.getBusinessRegistrationNumber())
                        .registrationCountry(company.getRegistrationCountry())
                        .registrationDate(company.getRegistrationDate())
                        .industryType(company.getIndustryType())
                        .companySize(company.getCompanySize())
                        .companyDescription(company.getCompanyDescription())
                        .registeredCompanyAddress(company.getRegisteredCompanyAddress())
                        .businessPhoneNumber(company.getBusinessPhoneNumber())
                        .businessEmail(company.getBusinessEmail())
                        .officialWebsiteUrl(company.getOfficialWebsiteUrl())
                        .taxNumber(company.getTaxNumber())
                        .companyStatus(company.getCompanyStatus())
                        .creatorId(company.getCreatorId())
                        .approvedBy(company.getApprovedBy())
                        .employerInvolved(company.getEmployerInvolved());

                employerProfileRepository.findByEmployer_EmployerUserId(company.getCreatorId())
                        .ifPresent(employer -> dto.creatorName(employer.getFullName()));

                if (company.getApprovedBy() != null) {
                        adminProfileRepository.findByAdmin_AdminUserId(company.getApprovedBy())
                                .ifPresent(admin -> dto.approvedByName(admin.getFullName()));
                }

                companyAttachmentRepository.findByCompanyId(company.getCompanyId()).ifPresent(attachment -> {
                        dto.companyLogo(toDTO(attachment.getCompanyLogo()));
                        dto.companyCert(toDTO(attachment.getCompanyCert()));
                        dto.businessLicense(toDTO(attachment.getBusinessLicense()));
                });

                return dto.build();
                }).collect(Collectors.toList());
    }

    private CompanyResponseDTO.FileDataDTO toDTO(CompanyAttachment.FileData fileData) {
        if (fileData == null) return null;
        return CompanyResponseDTO.FileDataDTO.builder()
                .fileName(fileData.getFileName())
                .fileBytes("data:" + fileData.getContentType() + ";base64," +
                        Base64.getEncoder().encodeToString(fileData.getFileBytes()))
                .contentType(fileData.getContentType())
                .build();
    }


        @Autowired
        JavaMailSender mailSender;

        public void notifyCompanyStatus(Company company, String reason) {
                System.out.println("notifyCompanyStatus called for companyId: " + company.getCompanyId());

                employerProfileRepository.findByEmployer_EmployerUserId(company.getCreatorId())
                        .ifPresentOrElse(creator -> {
                        String creatorEmail = creator.getEmployer().getEmail();
                        String creatorName = creator.getEmployer().getProfile().getFullName();

                        try {
                                MimeMessage message = mailSender.createMimeMessage();
                                MimeMessageHelper helper = new MimeMessageHelper(message, true);

                                String body = "<div style='font-family:Arial,sans-serif;font-size:14px;color:#333;'>"
                                + "<p>Dear " + creatorName + ",</p>"
                                + "<p>The status of your company <strong>" + company.getCompanyName() + "</strong> has been updated.</p>"
                                + "<p><strong>New Status:</strong> " + capitalize(company.getCompanyStatus()) + "</p>"
                                + (reason != null && !reason.isBlank() ? "<p><strong>Reason:</strong> " + reason + "</p>" : "")
                                + "<br>"
                                + "<p>Please login to your account to view more details.</p>"
                                + "<br>"
                                + "<p>Regards,<br>GigsUniverse Team</p>"
                                + "</div>";

                                helper.setFrom("admin@gigsuniverse.studio");
                                helper.setTo(creatorEmail);
                                helper.setSubject("Company Status Updated - GigsUniverse");
                                helper.setText(body, true);

                                System.out.println("Sending email to: " + creatorEmail);
                                mailSender.send(message);
                                System.out.println("Email sent successfully!");

                        } catch (MessagingException e) {
                                System.err.println("Failed to send email to: " + creatorEmail);
                                e.printStackTrace();
                        }

                        }, () -> System.err.println("Creator not found for ID: " + company.getCreatorId()));
                }


        private String capitalize(String str) {
                if (str == null || str.isBlank()) return str;
                return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }

     public Company updateCompanyStatus(int companyId, String newStatus, String reason, String adminId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        switch (newStatus.toLowerCase()) {
                case "terminated" -> {
                company.setCompanyStatus("terminated");
                company.setApprovedBy(adminId);
                companyRepository.save(company);
                notifyCompanyStatus(company, reason);
                }
                case "verified" -> {
                company.setCompanyStatus("verified");
                company.setApprovedBy(adminId);
                companyRepository.save(company);

                }
                case "pending" -> {
                company.setCompanyStatus("pending");
                company.setApprovedBy(null);
                companyRepository.save(company);
                }
                default -> throw new IllegalArgumentException("Invalid status: " + newStatus);
        }

                return company;
        }
}
