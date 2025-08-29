package com.giguniverse.backend.Company.Repository;

import com.giguniverse.backend.Company.Model.CompanyImage;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface CompanyImageRepository extends MongoRepository<CompanyImage, String> {

    Optional<CompanyImage> findByCompanyId(Integer companyId);
    void deleteByCompanyId(Integer companyId);

}
