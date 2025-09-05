package com.giguniverse.backend.Company.Controller;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Company.Model.AvailableUserDTO;
import com.giguniverse.backend.Company.Model.Company;
import com.giguniverse.backend.Company.Model.CompanyResponseDTO;
import com.giguniverse.backend.Company.Model.ProfileCompanyDTO;
import com.giguniverse.backend.Company.Model.StatusUpdateRequest;
import com.giguniverse.backend.Company.Model.UserInvolvementDTO;
import com.giguniverse.backend.Company.Model.VerifiedCompanyDTO;
import com.giguniverse.backend.Company.Service.CompanyService;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCompany(
            @RequestParam String companyName,
            @RequestParam String businessRegistrationNumber,
            @RequestParam String registrationCountry,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date registrationDate,
            @RequestParam String industryType,
            @RequestParam String companySize,
            @RequestParam String companyDescription,
            @RequestParam String registeredCompanyAddress,
            @RequestParam String businessPhoneNumber,
            @RequestParam String businessEmail,
            @RequestParam(required = false) String officialWebsiteUrl,
            @RequestParam(required = false) String taxNumber,
            @RequestParam(required = false) MultipartFile incorporationCertificate,
            @RequestParam(required = false) MultipartFile businessLicense,
            @RequestParam(required = false) MultipartFile companyLogo) throws IOException {
        Company saved = companyService.createCompany(
                companyName, businessRegistrationNumber, registrationCountry, registrationDate,
                industryType, companySize, companyDescription,
                registeredCompanyAddress, businessPhoneNumber, businessEmail, officialWebsiteUrl, taxNumber,
                incorporationCertificate, businessLicense, companyLogo);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/my-registered-company")
    public ResponseEntity<?> getMyCompanies() {
        String userId = AuthUtil.getUserId();
        List<Company> companies = companyService.getCompaniesByCreatorId(userId);

        return ResponseEntity.ok(companies);
    }

    @GetMapping("/verified-companies")
    public List<VerifiedCompanyDTO> getVerifiedCompanies() {
        return companyService.getVerifiedCompanies();
    }

    @GetMapping("/user-involvement")
    public ResponseEntity<?> getUserInvolvement() {
        String userId = AuthUtil.getUserId();
        UserInvolvementDTO dto = companyService.getUserInvolvement(userId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{companyId}/company-info")
    public ResponseEntity<?> getCompanyWithMediaAndEmployees(@PathVariable int companyId) {
        return companyService.getCompanyWithMediaAndEmployees(companyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available-employers")
    public ResponseEntity<List<AvailableUserDTO>> getAvailableEmployers() {
        List<AvailableUserDTO> availableEmployers = companyService.getAvailableEmployers();
        return ResponseEntity.ok(availableEmployers);
    }

    @PutMapping(value = "/{companyId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCompany(
            @PathVariable Integer companyId,

            // scalar fields
            @RequestParam String companyName,
            @RequestParam String businessRegistrationNumber,
            @RequestParam String registrationCountry,
            @RequestParam(required = false) String registrationDate,
            @RequestParam String industryType,
            @RequestParam String companySize,
            @RequestParam String companyDescription,
            @RequestParam String registeredCompanyAddress,
            @RequestParam String businessPhoneNumber,
            @RequestParam String businessEmail,
            @RequestParam String officialWebsiteUrl,
            @RequestParam String taxNumber,
            @RequestParam(required = false, defaultValue = "") String employerInvolved, // CSV

            // delete flags
            @RequestParam(value = "deleteVideo", required = false, defaultValue = "false") boolean deleteVideo,
            @RequestParam(value = "deleteImages", required = false, defaultValue = "false") boolean deleteImages,

            // files
            @RequestPart(value = "companyLogo", required = false) MultipartFile companyLogo,
            @RequestPart(value = "companyVideo", required = false) MultipartFile companyVideo,
            @RequestPart(value = "companyImage", required = false) List<MultipartFile> companyImages
    ) throws IOException {

        // --- Build map for service ---
        Map<String, String> fields = new HashMap<>();
        fields.put("companyId", companyId.toString());
        fields.put("companyName", companyName);
        fields.put("businessRegistrationNumber", businessRegistrationNumber);
        fields.put("registrationCountry", registrationCountry);
        fields.put("registrationDate", registrationDate);
        fields.put("industryType", industryType);
        fields.put("companySize", companySize);
        fields.put("companyDescription", companyDescription);
        fields.put("registeredCompanyAddress", registeredCompanyAddress);
        fields.put("businessPhoneNumber", businessPhoneNumber);
        fields.put("businessEmail", businessEmail);
        fields.put("officialWebsiteUrl", officialWebsiteUrl);
        fields.put("taxNumber", taxNumber);
        fields.put("employerInvolved", employerInvolved);

        // --- Call service directly ---
        companyService.updateCompanyAndMedia(
                fields,
                companyLogo,
                companyImages,
                companyVideo,
                deleteImages,
                deleteVideo
        );

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{companyId}/leave")
    public ResponseEntity<?> leaveCompany(@PathVariable Integer companyId) {
        String userId = AuthUtil.getUserId(); 
        companyService.leaveCompany(companyId, userId);
        return ResponseEntity.ok("You have left the company.");
    }

    @GetMapping("/is-involved")
    public ResponseEntity<?> checkIfUserInvolvedInAnyCompany() {
        boolean involved = companyService.isUserInvolvedInAnyCompany();
        return ResponseEntity.ok(Map.of("involved", involved));
    }

    @GetMapping("/my-profile-company")
    public ResponseEntity<ProfileCompanyDTO> getUserCompany() {
        String userId = AuthUtil.getUserId(); 
        ProfileCompanyDTO companyInfo = companyService.getUserCompany(userId);

        if (companyInfo != null) {
            return ResponseEntity.ok(companyInfo);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/view-user-company/{userId}")
    public ResponseEntity<ProfileCompanyDTO> getViewUserCompany(@PathVariable String userId) {
        ProfileCompanyDTO companyInfo = companyService.getViewUserCompany(userId);

        if (companyInfo != null) {
            return ResponseEntity.ok(companyInfo);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/get-company-info/{companyId}")
    public ResponseEntity<VerifiedCompanyDTO> getCompanyInfo(@PathVariable String companyId) {
        VerifiedCompanyDTO dto = companyService.getVerifiedCompanyById(companyId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/admin-verification")
    public ResponseEntity<List<CompanyResponseDTO>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompaniesWithAttachments());
    }

    @PutMapping("/{companyId}/status")
    public ResponseEntity<Map<String, Object>> updateCompanyStatus(
            @PathVariable int companyId,
            @RequestBody StatusUpdateRequest request
    ) {

        String userId = AuthUtil.getUserId();

        Company updatedCompany = companyService.updateCompanyStatus(
                companyId,
                request.getNewStatus(),
                request.getReason(),
                userId
        );

        Map<String, Object> response = new HashMap<>();
        response.put("companyId", updatedCompany.getCompanyId());
        response.put("status", updatedCompany.getCompanyStatus());
        response.put("approvedBy", updatedCompany.getApprovedBy());
        response.put("reason", request.getReason() != null ? request.getReason() : "");

        return ResponseEntity.ok(response);
    }
}
