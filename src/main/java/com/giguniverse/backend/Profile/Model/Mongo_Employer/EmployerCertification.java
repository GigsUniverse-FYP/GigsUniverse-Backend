package com.giguniverse.backend.Profile.Model.Mongo_Employer;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "employer_certification")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployerCertification {
    @Id
    private String id; 

    private String userId;
    private String fileName;
    private byte[] fileData;
    private String contentType; 
}
