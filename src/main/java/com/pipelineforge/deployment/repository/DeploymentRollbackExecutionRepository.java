package com.pipelineforge.deployment.repository;

import com.pipelineforge.deployment.entity.DeploymentRollbackExecution;
import com.pipelineforge.deployment.entity.DeploymentRollbackExecutionStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRollbackExecutionRepository extends JpaRepository<DeploymentRollbackExecution, UUID> {
	boolean existsByPipelineIdAndStatus(UUID pipelineId, DeploymentRollbackExecutionStatus status);

	Page<DeploymentRollbackExecution> findByPipelineId(UUID pipelineId, Pageable pageable);
}
