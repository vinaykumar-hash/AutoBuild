package com.pipelineforge.deployment.dto;

import com.pipelineforge.deployment.entity.DeploymentLogLevel;
import com.pipelineforge.deployment.entity.DeploymentRollbackLog;
import java.time.Instant;
import java.util.UUID;

public record RollbackLogResponse(
		UUID id,
		UUID rollbackId,
		String message,
		Instant timestamp,
		DeploymentLogLevel level
) {
	public static RollbackLogResponse from(DeploymentRollbackLog log) {
		return new RollbackLogResponse(
				log.getId(),
				log.getRollbackId(),
				log.getMessage(),
				log.getTimestamp(),
				log.getLevel()
		);
	}
}
