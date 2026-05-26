package com.pipelineforge.pipeline.dto;

import com.pipelineforge.pipeline.validation.UniqueExecutionOrder;
import com.pipelineforge.pipeline.validation.UniqueStageName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record PipelineCreateRequest(
		@NotBlank(message = "pipelineName is required")
		@Size(max = 140, message = "pipelineName must be 140 characters or fewer")
		String pipelineName,
		@NotNull(message = "repositoryId is required")
		UUID repositoryId,
		@NotEmpty(message = "stages are required")
		@UniqueStageName
		@UniqueExecutionOrder
		List<@Valid PipelineStageRequest> stages
) {
}
