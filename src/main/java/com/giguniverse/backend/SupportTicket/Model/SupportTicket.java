package com.giguniverse.backend.SupportTicket.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
@Entity
@Table(name = "support_ticket")
public class SupportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int supportTicketId; 

    private String ticketSubject;

    private String ticketCategory;

    @Column(columnDefinition = "TEXT")
    private String ticketDescription;

    private String ticketStatus; // open, in_progress, closed, resolved

    private String ticketPriority; // low, medium, high, premium

    private LocalDateTime ticketCreationDate;

    private LocalDateTime ticketUpdateDate;

    // Track who created the ticket
    private String creatorId;  // id of employer or freelancer

    private String creatorType; // user role

    private String adminId; // optional, can be null if not yet assigned

}
