package com.giguniverse.backend.Company.Model;

public class ProfileCompanyDTO {
    private int companyId;
    private String companyName;
    private String role; // "creator" or "employer"

    public ProfileCompanyDTO(int companyId, String companyName, String role) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.role = role;
    }

    // Getters
    public int getCompanyId() { return companyId; }
    public String getCompanyName() { return companyName; }
    public String getRole() { return role; }
}
