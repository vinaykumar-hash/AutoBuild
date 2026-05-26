package com.pipelineforge.deployment.service;

import com.pipelineforge.deployment.dto.DeploymentJobEvent;
import com.pipelineforge.deployment.dto.DeploymentLogResponse;
import com.pipelineforge.deployment.dto.DeploymentMetricsResponse;
import com.pipelineforge.deployment.dto.DeploymentResponse;
import com.pipelineforge.deployment.dto.PagedResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface DeploymentService {
	DeploymentResponse get(UUID id);

	PagedResponse<DeploymentResponse> list(UUID pipelineId, String status, String stage, Instant from, Instant to, Pageable pageable);

	PagedResponse<DeploymentLogResponse> logs(UUID deploymentId, Pageable pageable);

	DeploymentMetricsResponse metrics(Instant from, Instant to);

	UUID startDeployment(DeploymentJobEvent event);

	void markSuccess(UUID deploymentId);

	void markFailed(UUID deploymentId, String errorMessage);
}
