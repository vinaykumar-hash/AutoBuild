package com.pipelineforge.deployment.dto;

import com.pipelineforge.deployment.entity.DeploymentRollbackExecution;
import com.pipelineforge.deployment.entity.DeploymentRollbackExecutionStatus;
import java.time.Instant;
import java.util.UUID;

public record RollbackResponse(
		UUID id,
		UUID pipelineId,
		UUID fromDeploymentId,
		UUID toDeploymentId,
		long fromVersion,
		long toVersion,
		DeploymentRollbackExecutionStatus status,
		Instant startedAt,
		Instant completedAt
) {
	public static RollbackResponse from(DeploymentRollbackExecution rollback, long fromVersion, long toVersion) {
		return new RollbackResponse(
				rollback.getId(),
				rollback.getPipelineId(),
				rollback.getFromDeploymentId(),
				rollback.getToDeploymentId(),
				fromVersion,
				toVersion,
				rollback.getStatus(),
				rollback.getStartedAt(),
				rollback.getCompletedAt()
		);
	}
}
