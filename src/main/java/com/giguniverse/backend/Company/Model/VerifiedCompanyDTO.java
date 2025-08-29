package com.giguniverse.backend.Company.Model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifiedCompanyDTO{
    private Company company;
    private CompanyAttachment attachment;
    private CompanyImage image;
    private CompanyVideo video;
}
