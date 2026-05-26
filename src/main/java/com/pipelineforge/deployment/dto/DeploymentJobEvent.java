package com.pipelineforge.deployment.dto;

import java.time.Instant;
import java.util.UUID;

public record DeploymentJobEvent(
		String messageId,
		UUID pipelineId,
		String triggeredBy,
		Instant triggeredAt,
		Long pipelineVersion,
		String commitHash
) {
	public static DeploymentJobEvent create(UUID pipelineId, String triggeredBy, Long pipelineVersion, String commitHash) {
		String hash = commitHash == null || commitHash.isBlank() ? "unknown" : commitHash;
		return new DeploymentJobEvent(UUID.randomUUID().toString(), pipelineId, triggeredBy, Instant.now(), pipelineVersion, hash);
	}
}
