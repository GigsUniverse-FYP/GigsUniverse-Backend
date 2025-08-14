package com.giguniverse.backend.Profile.Repository.Mongo_Freelancer;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerCertification;

import java.util.List;

@Repository
public interface FreelancerCertificationRepository extends MongoRepository<FreelancerCertification, String> {
    List<FreelancerCertification> findByUserId(String userId);
    void deleteByUserId(String userId);
}

