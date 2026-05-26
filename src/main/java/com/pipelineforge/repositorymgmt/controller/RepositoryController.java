package com.pipelineforge.repositorymgmt.controller;

import com.pipelineforge.repositorymgmt.dto.PagedResponse;
import com.pipelineforge.repositorymgmt.dto.RepositoryCreateRequest;
import com.pipelineforge.repositorymgmt.dto.RepositoryResponse;
import com.pipelineforge.repositorymgmt.dto.RepositoryUpdateRequest;
import com.pipelineforge.repositorymgmt.service.RepositoryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/repositories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','RELEASE_MANAGER')")
public class RepositoryController {
	private final RepositoryService repositoryService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RepositoryResponse create(@Valid @RequestBody RepositoryCreateRequest request) {
		return repositoryService.create(request);
	}

	@GetMapping("/{id}")
	public RepositoryResponse get(@PathVariable UUID id) {
		return repositoryService.get(id);
	}

	@GetMapping
	public PagedResponse<RepositoryResponse> list(
			@RequestParam(required = false) String repoName,
			@RequestParam(required = false) String branch,
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
		return repositoryService.list(repoName, branch, createdBy, pageable);
	}

	@PutMapping("/{id}")
	public RepositoryResponse update(@PathVariable UUID id, @Valid @RequestBody RepositoryUpdateRequest request) {
		return repositoryService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('ADMIN','RELEASE_MANAGER')")
	public void delete(@PathVariable UUID id) {
		repositoryService.delete(id);
	}
}
