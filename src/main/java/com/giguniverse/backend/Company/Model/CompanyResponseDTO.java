package com.giguniverse.backend.Company.Model;


import lombok.*;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompanyResponseDTO {
    private int companyId;
    private String companyName;
    private String businessRegistrationNumber;
    private String registrationCountry;
    private Date registrationDate;
    private String industryType;
    private String companySize;
    private String companyDescription;
    private String registeredCompanyAddress;
    private String businessPhoneNumber;
    private String businessEmail;
    private String officialWebsiteUrl;
    private String taxNumber;
    private String companyStatus;

    private String creatorId;
    private String creatorName;

    private String approvedBy;
    private String approvedByName;

    private String employerInvolved;

    private FileDataDTO companyLogo;
    private FileDataDTO companyCert;
    private FileDataDTO businessLicense;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class FileDataDTO {
        private String fileName;
        private String fileBytes;  
        private String contentType;
    }
}