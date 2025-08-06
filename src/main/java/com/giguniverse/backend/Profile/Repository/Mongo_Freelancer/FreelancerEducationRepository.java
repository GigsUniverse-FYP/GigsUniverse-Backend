package com.giguniverse.backend.Profile.Repository.Mongo_Freelancer;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerEducation;

import java.util.List;

@Repository
public interface FreelancerEducationRepository extends MongoRepository<FreelancerEducation, String> {
    List<FreelancerEducation> findByUserId(String userId);
}
