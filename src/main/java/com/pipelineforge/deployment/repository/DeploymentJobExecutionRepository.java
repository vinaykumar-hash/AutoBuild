package com.pipelineforge.deployment.repository;

import com.pipelineforge.deployment.entity.DeploymentJobExecution;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentJobExecutionRepository extends JpaRepository<DeploymentJobExecution, UUID> {
	Optional<DeploymentJobExecution> findByMessageId(String messageId);

	boolean existsByMessageId(String messageId);
}
