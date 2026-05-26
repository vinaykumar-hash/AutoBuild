package com.pipelineforge.pipeline.dto;

import com.pipelineforge.pipeline.entity.Pipeline;
import com.pipelineforge.pipeline.entity.PipelineStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PipelineResponse(
		UUID id,
		String pipelineName,
		UUID repositoryId,
		PipelineStatus status,
		Long version,
		Instant createdAt,
		String createdBy,
		Instant lastTriggeredAt,
		Instant lastExecutionStartedAt,
		Instant lastExecutionFinishedAt,
		List<PipelineStageResponse> stages
) {
	public static PipelineResponse from(Pipeline pipeline) {
		return new PipelineResponse(
				pipeline.getId(),
				pipeline.getPipelineName(),
				pipeline.getRepositoryId(),
				pipeline.getStatus(),
				pipeline.getVersion(),
				pipeline.getCreatedAt(),
				pipeline.getCreatedBy(),
				pipeline.getLastTriggeredAt(),
				pipeline.getLastExecutionStartedAt(),
				pipeline.getLastExecutionFinishedAt(),
				pipeline.getStages().stream()
						.map(PipelineStageResponse::from)
						.toList()
		);
	}
}
