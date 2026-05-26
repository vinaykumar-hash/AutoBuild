package com.pipelineforge.pipeline.controller;

import com.pipelineforge.pipeline.dto.PagedResponse;
import com.pipelineforge.pipeline.dto.PipelineCreateRequest;
import com.pipelineforge.pipeline.dto.PipelineResponse;
import com.pipelineforge.pipeline.service.PipelineService;
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
@RequestMapping("/api/v1/pipelines")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','RELEASE_MANAGER')")
public class PipelineController {
	private final PipelineService pipelineService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN','RELEASE_MANAGER')")
	public PipelineResponse create(@Valid @RequestBody PipelineCreateRequest request) {
		return pipelineService.create(request);
	}

	@GetMapping("/{id}")
	public PipelineResponse get(@PathVariable UUID id) {
		return pipelineService.get(id);
	}

	@GetMapping
	public PagedResponse<PipelineResponse> list(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) UUID repositoryId,
			@RequestParam(required = false) String createdBy,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort
	) {
		String[] sortParts = sort.split(",");
		Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
				? Sort.Direction.ASC
				: Sort.Direction.DESC;
		String sortField = sortParts[0];
		Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
		return pipelineService.list(status, repositoryId, createdBy, pageable);
	}

	@PostMapping("/{id}/trigger")
	@PreAuthorize("hasAnyRole('ADMIN','RELEASE_MANAGER')")
	public PipelineResponse trigger(@PathVariable UUID id) {
		return pipelineService.trigger(id);
	}
}
