package com.giguniverse.backend.SupportTicket.Controller;

import com.giguniverse.backend.SupportTicket.Model.SupportTicket;
import com.giguniverse.backend.SupportTicket.Model.TicketInfoAttachmentDTO;
import com.giguniverse.backend.SupportTicket.Repository.SupportTicketRepository;
import com.giguniverse.backend.SupportTicket.Service.SupportTicketService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tickets")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @GetMapping("/user-info")
    public ResponseEntity<?> getCurrentUserInfo() {
        return ResponseEntity.ok(supportTicketService.getCurrentUserInfo());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTicket(
            @RequestParam("subject") String subject,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam("priority") String priority,
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments) {
        try {
            SupportTicket savedTicket = supportTicketService.createTicket(subject, description, category, priority,
                    attachments);
            return ResponseEntity.ok(Map.of(
                    "ticketId", savedTicket.getSupportTicketId(),
                    "message", "Ticket created successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status-counts")
    public ResponseEntity<List<SupportTicketRepository.StatusCount>> getTicketStatusCounts() {
        return ResponseEntity.ok(supportTicketService.getTicketStatusCounts());
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<List<Map<String, Object>>> getMyTickets() {
        return ResponseEntity.ok(supportTicketService.getTicketsForCurrentUser());
    }

    @GetMapping("/all-tickets")
    public ResponseEntity<List<TicketInfoAttachmentDTO>> getAllTicketsWithAttachments() {
        return ResponseEntity.ok(supportTicketService.getAllTicketsWithAttachments());
    }

    @GetMapping("/admin-info")
    public ResponseEntity<Map<String, String>> getCurrentAdmin() {
        return ResponseEntity.ok(supportTicketService.getCurrentAdmin());
    }

    @PostMapping("/assign/{ticketId}")
    public ResponseEntity<String> assignTicket(
            @PathVariable Integer ticketId,
            @RequestBody Map<String, String> body) {

        String adminId = body.get("adminId");
        if (adminId == null || adminId.isBlank()) {
            return ResponseEntity.badRequest().body("AdminId is required");
        }

        supportTicketService.assignTicket(ticketId, adminId);
        return ResponseEntity.ok("Ticket assigned successfully");
    }

    @PutMapping("/{ticketId}/status")
    public ResponseEntity<?> updateTicketStatus(
            @PathVariable String ticketId,
            @RequestBody Map<String, String> request) {
        String newStatus = request.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
        }

        SupportTicket updatedTicket = supportTicketService.updateStatus(ticketId, newStatus);

        return ResponseEntity.ok(Map.of(
                "ticketId", updatedTicket.getSupportTicketId(),
                "status", updatedTicket.getTicketStatus()));
    }
}