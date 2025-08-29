package com.giguniverse.backend.Company.Model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.util.Date;

import jakarta.persistence.*;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
@Entity
@Table(name = "company")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int companyId;

    // Registration Section
    private String companyName;
    private String businessRegistrationNumber;
    private String registrationCountry;
    private Date registrationDate;

    private String industryType;
    private String companySize;
    @Column(columnDefinition = "TEXT")
    private String companyDescription;

    private String registeredCompanyAddress;
    private String businessPhoneNumber;
    private String businessEmail;
    private String officialWebsiteUrl;
    private String taxNumber;

    private String companyStatus; // pending, verified, terminated

    private String creatorId; // employerId

    @Column(columnDefinition = "TEXT")
    private String employerInvolved; // list of employer Ids

    private String approvedBy; // adminId


    // Mongo
    // - company logo + company cert + business license (3 in 1)
    // - company image (3 in 1)
    // - company video (1 in 1)
}
