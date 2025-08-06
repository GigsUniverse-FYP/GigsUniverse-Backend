package com.giguniverse.backend.Profile.Model.Mongo_Freelancer;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "freelancer_certification")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FreelancerCertification {
    
    @Id
    private String id; 

    private String userId;
    private String fileName;
    private byte[] fileData;
    private String contentType; 
}