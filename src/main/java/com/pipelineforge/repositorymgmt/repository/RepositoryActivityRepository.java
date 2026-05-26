package com.pipelineforge.repositorymgmt.repository;

import com.pipelineforge.repositorymgmt.entity.RepositoryActivity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RepositoryActivityRepository extends JpaRepository<RepositoryActivity, UUID>, JpaSpecificationExecutor<RepositoryActivity> {
}
