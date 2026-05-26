package com.pipelineforge.deployment.dto;

import com.pipelineforge.deployment.entity.Deployment;
import com.pipelineforge.deployment.entity.DeploymentStatus;
import java.time.Instant;
import java.util.UUID;

public record DeploymentResponse(
		UUID id,
		UUID pipelineId,
		String commitHash,
		long versionNumber,
		boolean stable,
		DeploymentStatus status,
		String currentStage,
		Instant startedAt,
		Instant completedAt,
		Long durationMs
) {
	public static DeploymentResponse from(Deployment deployment) {
		return new DeploymentResponse(
				deployment.getId(),
				deployment.getPipelineId(),
				deployment.getCommitHash(),
				deployment.getVersionNumber(),
				deployment.isStable(),
				deployment.getStatus(),
				deployment.getCurrentStage(),
				deployment.getStartedAt(),
				deployment.getCompletedAt(),
				deployment.getDurationMs()
		);
	}
}
