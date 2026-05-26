package com.pipelineforge.deployment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RollbackRequest(
		@NotNull(message = "pipelineId is required")
		UUID pipelineId,
		@NotNull(message = "targetDeploymentId is required")
		UUID targetDeploymentId,
		@Size(max = 1000, message = "reason must be 1000 characters or fewer")
		String reason
) {
}
