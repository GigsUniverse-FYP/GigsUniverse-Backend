package com.giguniverse.backend.Company.Repository;

import com.giguniverse.backend.Company.Model.CompanyVideo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyVideoRepository extends MongoRepository<CompanyVideo, String> {
    Optional<CompanyVideo> findByCompanyId(Integer companyId);
    void deleteByCompanyId(Integer companyId);
}
