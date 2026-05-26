package com.pipelineforge.deployment.controller;

import com.pipelineforge.deployment.dto.DeploymentLogResponse;
import com.pipelineforge.deployment.dto.DeploymentMetricsResponse;
import com.pipelineforge.deployment.dto.DeploymentResponse;
import com.pipelineforge.deployment.dto.PagedResponse;
import com.pipelineforge.deployment.service.DeploymentService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','RELEASE_MANAGER')")
public class DeploymentHistoryController {
	private final DeploymentService deploymentService;

	@GetMapping("/{id}")
	public DeploymentResponse get(@PathVariable UUID id) {
		return deploymentService.get(id);
	}

	@GetMapping
	public PagedResponse<DeploymentResponse> list(
			@RequestParam(required = false) UUID pipelineId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String stage,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "startedAt,desc") String sort
	) {
		String[] sortParts = sort.split(",");
		Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
				? Sort.Direction.ASC
				: Sort.Direction.DESC;
		String sortField = sortParts[0];
		Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
		return deploymentService.list(pipelineId, status, stage, from, to, pageable);
	}

	@GetMapping("/metrics")
	public DeploymentMetricsResponse metrics(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
	) {
		return deploymentService.metrics(from, to);
	}

	@GetMapping("/{id}/logs")
	public PagedResponse<DeploymentLogResponse> logs(
			@PathVariable UUID id,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size
	) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "timestamp"));
		return deploymentService.logs(id, pageable);
	}
}
