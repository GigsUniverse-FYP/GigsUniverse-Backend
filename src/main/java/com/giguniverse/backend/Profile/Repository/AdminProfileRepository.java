package com.giguniverse.backend.Profile.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Auth.Model.Admin;
import com.giguniverse.backend.Profile.Model.AdminProfile;

@Repository
public interface AdminProfileRepository extends JpaRepository<AdminProfile, Integer> {
    Optional<AdminProfile> findByAdmin_AdminUserId(String adminUserId);
    Optional<AdminProfile> findByAdmin(Admin admin);

}
