package com.giguniverse.backend.Company.Model;

import lombok.*;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "company_image")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompanyImage {
    @Id
    private String id; 

    private Integer companyId;

    private List<FileData> companyImages;

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
