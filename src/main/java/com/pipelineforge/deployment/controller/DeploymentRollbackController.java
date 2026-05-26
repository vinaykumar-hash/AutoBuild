package com.pipelineforge.deployment.controller;

import com.pipelineforge.deployment.dto.PagedResponse;
import com.pipelineforge.deployment.dto.RollbackLogResponse;
import com.pipelineforge.deployment.dto.RollbackRequest;
import com.pipelineforge.deployment.dto.RollbackResponse;
import com.pipelineforge.deployment.service.RollbackService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/deployments/rollbacks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RELEASE_MANAGER')")
public class DeploymentRollbackController {
	private final RollbackService rollbackService;

	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public RollbackResponse rollback(@Valid @RequestBody RollbackRequest request) {
		return rollbackService.rollback(request);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','RELEASE_MANAGER')")
	public PagedResponse<RollbackResponse> history(
			@RequestParam(required = false) UUID pipelineId,
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
		return rollbackService.history(pipelineId, pageable);
	}

	@GetMapping("/{id}/logs")
	@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','RELEASE_MANAGER')")
	public PagedResponse<RollbackLogResponse> logs(
			@PathVariable UUID id,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size
	) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "timestamp"));
		return rollbackService.logs(id, pageable);
	}
}
