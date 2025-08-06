package com.giguniverse.backend.Profile.Repository.Mongo_Employer;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Profile.Model.Mongo_Employer.EmployerJobExperience;

@Repository
public interface EmployerJobExperienceRepository extends MongoRepository<EmployerJobExperience, String> {
    List<EmployerJobExperience> findByUserId(String userId);
}