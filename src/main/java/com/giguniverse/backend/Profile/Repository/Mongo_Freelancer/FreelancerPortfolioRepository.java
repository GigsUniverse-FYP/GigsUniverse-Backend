package com.giguniverse.backend.Profile.Repository.Mongo_Freelancer;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Profile.Model.Mongo_Freelancer.FreelancerPortfolio;

import java.util.List;

@Repository
public interface FreelancerPortfolioRepository extends MongoRepository<FreelancerPortfolio, String> {
    List<FreelancerPortfolio> findByUserId(String userId);
    void deleteByUserId(String userId);
}


