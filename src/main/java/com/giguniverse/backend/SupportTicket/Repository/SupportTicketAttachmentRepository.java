package com.giguniverse.backend.SupportTicket.Repository;

import com.giguniverse.backend.SupportTicket.Model.SupportTicketAttachment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketAttachmentRepository extends MongoRepository<SupportTicketAttachment, String> {

    List<SupportTicketAttachment> findBySupportTicketId(Integer supportTicketId);
    
}
