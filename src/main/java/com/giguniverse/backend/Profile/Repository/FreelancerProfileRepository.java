package com.giguniverse.backend.Profile.Repository;

import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Profile.Model.FreelancerProfile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FreelancerProfileRepository extends JpaRepository<FreelancerProfile, Integer> {
    Optional<FreelancerProfile> findByFreelancer_FreelancerUserId(String freelancerUserId);
    Optional<FreelancerProfile> findByFreelancer(Freelancer freelancer);

}