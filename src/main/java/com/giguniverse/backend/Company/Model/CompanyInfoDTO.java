package com.giguniverse.backend.Company.Model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyInfoDTO {
    private Company company;
    private CompanyAttachment attachment;
    private CompanyImage image;
    private CompanyVideo video;
    private List<EmployeeDTO> employees; 
}
