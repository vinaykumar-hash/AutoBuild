package com.pipelineforge.pipeline.dto;

import com.pipelineforge.pipeline.entity.PipelineStage;
import com.pipelineforge.pipeline.entity.PipelineStageName;
import com.pipelineforge.pipeline.entity.PipelineStageStatus;
import java.time.Instant;
import java.util.UUID;

public record PipelineStageResponse(
		UUID id,
		PipelineStageName stageName,
		int executionOrder,
		PipelineStageStatus status,
		Instant startedAt,
		Instant finishedAt
) {
	public static PipelineStageResponse from(PipelineStage stage) {
		return new PipelineStageResponse(
				stage.getId(),
				stage.getStageName(),
				stage.getExecutionOrder(),
				stage.getStatus(),
				stage.getStartedAt(),
				stage.getFinishedAt()
		);
	}
}
