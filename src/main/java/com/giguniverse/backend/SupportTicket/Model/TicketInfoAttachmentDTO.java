package com.giguniverse.backend.SupportTicket.Model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class TicketInfoAttachmentDTO {
  private SupportTicketDTO ticket;
  private List<TicketAttachmentDTO> attachments;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SupportTicketDTO {
        private int supportTicketId;
        private String ticketSubject;
        private String ticketCategory;
        private String ticketDescription;
        private String ticketStatus;
        private String ticketPriority;
        private LocalDateTime ticketCreationDate;
        private LocalDateTime ticketUpdateDate;
        private String creatorId;
        private String creatorType;
        private String adminId;
        private String creatorName;
        private String adminName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TicketAttachmentDTO {
        private String id;
        private Integer supportTicketId;
        private List<FileDataDTO> files;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class FileDataDTO {
        private String fileName;
        private String fileBase64; 
        private String contentType;
    }
}
