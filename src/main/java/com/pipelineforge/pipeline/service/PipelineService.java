package com.pipelineforge.pipeline.service;

import com.pipelineforge.pipeline.dto.PagedResponse;
import com.pipelineforge.pipeline.dto.PipelineCreateRequest;
import com.pipelineforge.pipeline.dto.PipelineResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface PipelineService {
	PipelineResponse create(PipelineCreateRequest request);

	PipelineResponse get(UUID id);

	PagedResponse<PipelineResponse> list(String status, UUID repositoryId, String createdBy, Pageable pageable);

	PipelineResponse trigger(UUID id);
}
