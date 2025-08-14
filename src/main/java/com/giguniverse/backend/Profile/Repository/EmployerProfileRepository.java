package com.giguniverse.backend.Profile.Repository;

import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Profile.Model.EmployerProfile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployerProfileRepository extends JpaRepository<EmployerProfile, Integer> {
    Optional<EmployerProfile> findByEmployer_EmployerUserId(String employerUserId);
    Optional<EmployerProfile> findByEmployer(Employer employer);

}
