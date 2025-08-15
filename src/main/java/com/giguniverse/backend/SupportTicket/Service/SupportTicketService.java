package com.giguniverse.backend.SupportTicket.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.SupportTicket.Model.SupportTicket;
import com.giguniverse.backend.SupportTicket.Model.SupportTicketAttachment;
import com.giguniverse.backend.SupportTicket.Repository.SupportTicketAttachmentRepository;
import com.giguniverse.backend.SupportTicket.Repository.SupportTicketRepository;

@Service
public class SupportTicketService {
    @Autowired
    private FreelancerRepository freelancerRepository;
    @Autowired
    private EmployerRepository employerRepository;

    @Autowired
    private SupportTicketRepository ticketRepo;
    @Autowired
    private SupportTicketAttachmentRepository attachmentRepo;

    public Map<String, Object> getCurrentUserInfo() {
        String userId = AuthUtil.getUserId();
        String userRole = AuthUtil.getUserRole();

        if (userRole.equalsIgnoreCase("freelancer")) {
            Optional<Freelancer> freelancerOpt = freelancerRepository.findById(userId);
            if (freelancerOpt.isPresent()) {
                Freelancer f = freelancerOpt.get();
                return Map.of(
                        "fullName", f.getProfile().getFullName(),
                        "email", f.getEmail(),
                        "phoneNumber", f.getProfile().getPhone(),
                        "userRole", userRole,
                        "isPremium", f.getProfile().getPremiumStatus());
            }
        } else if (userRole.equalsIgnoreCase("employer")) {
            Optional<Employer> employerOpt = employerRepository.findById(userId);
            if (employerOpt.isPresent()) {
                Employer e = employerOpt.get();
                return Map.of(
                        "fullName", e.getProfile().getFullName(),
                        "email", e.getEmail(),
                        "phoneNumber", e.getProfile().getPhone(),
                        "userRole", userRole,
                        "isPremium", e.getProfile().getPremiumStatus());
            }
        }

        // Fallback if user not found
        return Map.of(
                "fullName", "Unknown",
                "email", "",
                "phoneNumber", "",
                "userRole", userRole,
                "isPremium", false);
    }

    // creating new ticket
    public SupportTicket createTicket(
            String subject,
            String description,
            String category,
            String priority,
            List<MultipartFile> attachments) throws IOException {

        String userId = AuthUtil.getUserId();
        String userRole = AuthUtil.getUserRole();

        // --- Save ticket to PostgreSQL ---
        SupportTicket ticket = SupportTicket.builder()
                .ticketSubject(subject)
                .ticketCategory(category)
                .ticketDescription(description)
                .ticketPriority(priority)
                .ticketStatus("open")
                .ticketCreationDate(LocalDateTime.now())
                .ticketUpdateDate(LocalDateTime.now())
                .creatorId(userId)
                .creatorType(userRole)
                .build();

        SupportTicket savedTicket = ticketRepo.save(ticket);

        // --- Save attachments to MongoDB ---
        if (attachments != null && !attachments.isEmpty()) {
            List<SupportTicketAttachment.FileData> fileDataList = new ArrayList<>();
            for (MultipartFile file : attachments) {
                SupportTicketAttachment.FileData fd = SupportTicketAttachment.FileData.builder()
                        .fileName(file.getOriginalFilename())
                        .fileBytes(file.getBytes())
                        .contentType(file.getContentType())
                        .build();
                fileDataList.add(fd);
            }

            SupportTicketAttachment attachmentDoc = SupportTicketAttachment.builder()
                    .supportTicketId(savedTicket.getSupportTicketId())
                    .files(fileDataList)
                    .build();

            attachmentRepo.save(attachmentDoc);
        }

        return savedTicket;
    }


    public List<SupportTicketRepository.StatusCount> getTicketStatusCounts() {
        return ticketRepo.countTicketsByStatus();
    }

        public List<Map<String, Object>> getTicketsForCurrentUser() {

        String userId = AuthUtil.getUserId();

        // 1. Fetch tickets from PostgreSQL
        List<SupportTicket> tickets = ticketRepo.findByCreatorId(userId);

        List<Map<String, Object>> result = new ArrayList<>();

        for (SupportTicket ticket : tickets) {
            Map<String, Object> ticketData = new HashMap<>();
            ticketData.put("ticket", ticket);

            // 2. Fetch attachments from MongoDB
            List<SupportTicketAttachment> attachments =
                    attachmentRepo.findBySupportTicketId(ticket.getSupportTicketId());

            ticketData.put("attachments", attachments);

            result.add(ticketData);
        }

        return result;
    }
}
