package com.giguniverse.backend.SupportTicket.Model;

import lombok.*;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ticket_attachment")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupportTicketAttachment {
    
    @Id
    private String id; 

    private Integer supportTicketId;

    private List<FileData> files;

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