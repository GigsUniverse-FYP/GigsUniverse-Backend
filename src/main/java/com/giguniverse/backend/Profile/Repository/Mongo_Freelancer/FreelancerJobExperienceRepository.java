package com.giguniverse.backend.Profile.Repository.Mongo_Freelancer;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerJobExperience;

import java.util.List;

@Repository
public interface FreelancerJobExperienceRepository extends MongoRepository<FreelancerJobExperience, String> {
    List<FreelancerJobExperience> findByUserId(String userId);
}

