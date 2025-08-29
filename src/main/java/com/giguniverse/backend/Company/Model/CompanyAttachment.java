package com.giguniverse.backend.Company.Model;

import lombok.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "company_attachment")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompanyAttachment {
    @Id
    private String id; 

    private Integer companyId;

    private FileData companyLogo;

    private FileData companyCert;

    private FileData businessLicense;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class FileData {
        private String fileName;
        private byte[] fileBytes;
        private String contentType;
    }
}
