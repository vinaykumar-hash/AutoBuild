package com.pipelineforge.deployment.repository;

import com.pipelineforge.deployment.entity.DeploymentRollbackPoint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRollbackPointRepository extends JpaRepository<DeploymentRollbackPoint, UUID> {
	List<DeploymentRollbackPoint> findByDeploymentIdOrderByCreatedAtDesc(UUID deploymentId);
}
