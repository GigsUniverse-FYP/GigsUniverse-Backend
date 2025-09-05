package com.giguniverse.backend.SupportTicket.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Profile.Repository.AdminProfileRepository;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;
import com.giguniverse.backend.SupportTicket.Model.SupportTicket;
import com.giguniverse.backend.SupportTicket.Model.SupportTicketAttachment;
import com.giguniverse.backend.SupportTicket.Model.TicketInfoAttachmentDTO;
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

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepo;
    
    @Autowired
    private EmployerProfileRepository employerProfileRepo;

    @Autowired
    private AdminProfileRepository adminProfileRepo;

    

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
        String userId = AuthUtil.getUserId();
        return ticketRepo.countTicketsByStatusForUser(userId);
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

    public List<TicketInfoAttachmentDTO> getAllTicketsWithAttachments() {
        List<SupportTicket> tickets = ticketRepo.findAll();

        Map<String, Integer> statusOrder = Map.of(
            "open", 1,
            "in_progress", 2,
            "resolved", 3,
            "closed", 4
        );

        return tickets.stream()
                .map(ticket -> {
                    String creatorName = null;
                    if ("freelancer".equalsIgnoreCase(ticket.getCreatorType())) {
                        creatorName = freelancerProfileRepo.findByFreelancer_FreelancerUserId(ticket.getCreatorId())
                                .map(f -> f.getFullName())
                                .orElse("Unknown Freelancer");
                    } else if ("employer".equalsIgnoreCase(ticket.getCreatorType())) {
                        creatorName = employerProfileRepo.findByEmployer_EmployerUserId(ticket.getCreatorId())
                                .map(e -> e.getFullName())
                                .orElse("Unknown Employer");
                    }

                    String adminName = null;
                    if (ticket.getAdminId() != null) {
                        adminName = adminProfileRepo.findByAdmin_AdminUserId(ticket.getAdminId())
                                .map(a -> a.getFullName())
                                .orElse("Unassigned");
                    }

                    List<SupportTicketAttachment> attachments =
                            attachmentRepo.findBySupportTicketId(ticket.getSupportTicketId());

                    return TicketInfoAttachmentDTO.builder()
                            .ticket(
                                    TicketInfoAttachmentDTO.SupportTicketDTO.builder()
                                            .supportTicketId(ticket.getSupportTicketId())
                                            .ticketSubject(ticket.getTicketSubject())
                                            .ticketCategory(ticket.getTicketCategory())
                                            .ticketDescription(ticket.getTicketDescription())
                                            .ticketStatus(ticket.getTicketStatus())
                                            .ticketPriority(ticket.getTicketPriority())
                                            .ticketCreationDate(ticket.getTicketCreationDate())
                                            .ticketUpdateDate(ticket.getTicketUpdateDate())
                                            .creatorId(ticket.getCreatorId())
                                            .creatorType(ticket.getCreatorType())
                                            .creatorName(creatorName)
                                            .adminId(ticket.getAdminId())
                                            .adminName(adminName)
                                            .build()
                            )
                            .attachments(
                                    attachments.stream()
                                            .map(attachment ->
                                                    TicketInfoAttachmentDTO.TicketAttachmentDTO.builder()
                                                            .id(attachment.getId())
                                                            .supportTicketId(attachment.getSupportTicketId())
                                                            .files(
                                                                    attachment.getFiles().stream()
                                                                            .map(file ->
                                                                                    TicketInfoAttachmentDTO.FileDataDTO.builder()
                                                                                            .fileName(file.getFileName())
                                                                                            .fileBase64(Base64.getEncoder().encodeToString(file.getFileBytes()))
                                                                                            .contentType(file.getContentType())
                                                                                            .build()
                                                                            ).collect(Collectors.toList())
                                                            )
                                                            .build()
                                            ).collect(Collectors.toList())
                            )
                            .build();
                })
                .sorted(Comparator.comparingInt(
                    dto -> statusOrder.getOrDefault(dto.getTicket().getTicketStatus().toLowerCase(), 999)
                ))
                .collect(Collectors.toList());
        }


    public Map<String, String> getCurrentAdmin() {
        String currentAdminId = AuthUtil.getUserId();

        return adminProfileRepo.findByAdmin_AdminUserId(currentAdminId)
                .map(admin -> Map.of(
                        "adminId", currentAdminId,
                        "adminFullName", admin.getFullName()
                ))
                .orElseThrow(() -> new RuntimeException("Admin not found for id: " + currentAdminId));
    }

    public void assignTicket(Integer ticketId, String adminId) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // check if admin exists
        adminProfileRepo.findByAdmin_AdminUserId(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        ticket.setAdminId(adminId);
        ticket.setTicketStatus("in_progress");
        ticket.setTicketUpdateDate(LocalDateTime.now());

        ticketRepo.save(ticket);
    }

    public SupportTicket updateStatus(String ticketId, String newStatus) {
        SupportTicket ticket = ticketRepo.findById(Integer.valueOf(ticketId))
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        ticket.setTicketStatus(newStatus);
        return ticketRepo.save(ticket);
    }

}
