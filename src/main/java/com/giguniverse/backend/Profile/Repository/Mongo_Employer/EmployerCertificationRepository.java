package com.giguniverse.backend.Profile.Repository.Mongo_Employer;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerCertification;

@Repository
public interface EmployerCertificationRepository extends MongoRepository<EmployerCertification, String> {
    List<EmployerCertification> findByUserId(String userId);
    void deleteByUserId(String userId);
}
