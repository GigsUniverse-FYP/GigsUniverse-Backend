package com.giguniverse.backend.Company.Repository;

import com.giguniverse.backend.Company.Model.Company;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
    List<Company> findByCreatorId(String creatorId);
    List<Company> findByCompanyStatus(String companyStatus);
    boolean existsByCreatorId(String creatorId);
}
