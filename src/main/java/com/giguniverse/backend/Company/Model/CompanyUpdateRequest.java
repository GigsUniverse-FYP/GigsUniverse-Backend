package com.giguniverse.backend.Company.Model;

import lombok.Data;

@Data
public class CompanyUpdateRequest {
  public Long companyId;
  public String companyName;
  public String businessRegistrationNumber;
  public String registrationCountry;
  public String registrationDate; // ISO or yyyy-MM-dd
  public String industryType;
  public String companySize;
  public String companyDescription;
  public String registeredCompanyAddress;
  public String businessPhoneNumber;
  public String businessEmail;
  public String officialWebsiteUrl;
  public String taxNumber;
  public String employerInvolved; // CSV of emails
}
