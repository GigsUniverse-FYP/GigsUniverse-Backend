package com.giguniverse.backend.Company.Model;

import lombok.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "company_video")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompanyVideo {
    @Id
    private String id; 

    private Integer companyId;

    private FileData companyVideo;

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
