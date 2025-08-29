package com.giguniverse.backend.Company.Repository;

import com.giguniverse.backend.Company.Model.CompanyAttachment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyAttachmentRepository extends MongoRepository<CompanyAttachment, String> {

    Optional<CompanyAttachment> findByCompanyId(Integer companyId);
    void deleteByCompanyId(Integer companyId);

}
