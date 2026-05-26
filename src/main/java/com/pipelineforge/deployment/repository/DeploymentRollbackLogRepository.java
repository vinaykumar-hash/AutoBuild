package com.pipelineforge.deployment.repository;

import com.pipelineforge.deployment.entity.DeploymentRollbackLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRollbackLogRepository extends JpaRepository<DeploymentRollbackLog, UUID> {
	Page<DeploymentRollbackLog> findByRollbackId(UUID rollbackId, Pageable pageable);
}
