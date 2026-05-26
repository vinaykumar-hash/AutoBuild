package com.pipelineforge.pipeline.dto;

import com.pipelineforge.pipeline.entity.PipelineStageName;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PipelineStageRequest(
		@NotNull(message = "stageName is required")
		PipelineStageName stageName,
		@Min(value = 1, message = "executionOrder must be >= 1")
		int executionOrder
) {
}
