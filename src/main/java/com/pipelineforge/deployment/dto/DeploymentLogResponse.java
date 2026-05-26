package com.pipelineforge.deployment.dto;

import com.pipelineforge.deployment.entity.DeploymentLog;
import com.pipelineforge.deployment.entity.DeploymentLogLevel;
import java.time.Instant;
import java.util.UUID;

public record DeploymentLogResponse(
		UUID id,
		UUID deploymentId,
		String message,
		Instant timestamp,
		DeploymentLogLevel level
) {
	public static DeploymentLogResponse from(DeploymentLog log) {
		return new DeploymentLogResponse(
				log.getId(),
				log.getDeploymentId(),
				log.getMessage(),
				log.getTimestamp(),
				log.getLevel()
		);
	}
}
