package com.pipelineforge.deployment.repository;

import com.pipelineforge.deployment.entity.DeploymentLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentLogRepository extends JpaRepository<DeploymentLog, UUID> {
	Page<DeploymentLog> findByDeploymentId(UUID deploymentId, Pageable pageable);
}
