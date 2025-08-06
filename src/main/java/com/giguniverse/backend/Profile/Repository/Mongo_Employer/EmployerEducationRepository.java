package com.giguniverse.backend.Profile.Repository.Mongo_Employer;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerEducation;

import java.util.List;

@Repository
public interface EmployerEducationRepository extends MongoRepository<EmployerEducation, String> {
    List<EmployerEducation> findByUserId(String userId);
}
