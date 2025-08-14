package com.giguniverse.backend.Profile.Repository.Mongo_Freelancer;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerResume;

@Repository
public interface FreelancerResumeRepository extends MongoRepository<FreelancerResume, String> {
    List<FreelancerResume> findByUserId(String userId);
    void deleteByUserId(String userId);
}