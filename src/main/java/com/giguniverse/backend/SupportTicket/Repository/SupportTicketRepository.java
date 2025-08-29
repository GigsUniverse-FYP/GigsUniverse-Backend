package com.giguniverse.backend.SupportTicket.Repository;

import com.giguniverse.backend.SupportTicket.Model.SupportTicket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Integer> {
    List<SupportTicket> findByCreatorId(String creatorId);

    @Query("SELECT s.ticketStatus AS status, COUNT(s) AS count " +
           "FROM SupportTicket s GROUP BY s.ticketStatus")
    List<StatusCount> countTicketsByStatus();

    @Query("SELECT s.ticketStatus AS status, COUNT(s) AS count " +
           "FROM SupportTicket s WHERE s.creatorId = :creatorId GROUP BY s.ticketStatus")
    List<StatusCount> countTicketsByStatusForUser(String creatorId);

    interface StatusCount {
        String getStatus();
        Long getCount();
    }
}
