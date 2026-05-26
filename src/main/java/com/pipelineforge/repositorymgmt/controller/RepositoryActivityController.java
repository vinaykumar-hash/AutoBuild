package com.pipelineforge.repositorymgmt.controller;

import com.pipelineforge.repositorymgmt.dto.PagedResponse;
import com.pipelineforge.repositorymgmt.dto.RepositoryActivityResponse;
import com.pipelineforge.repositorymgmt.service.RepositoryActivityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/repositories/activity")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','RELEASE_MANAGER')")
public class RepositoryActivityController {
	private final RepositoryActivityService repositoryActivityService;

	@GetMapping
	public PagedResponse<RepositoryActivityResponse> list(
			@RequestParam(required = false) UUID repositoryId,
			@RequestParam(required = false) String action,
			@RequestParam(required = false) String actor,
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
		return repositoryActivityService.list(repositoryId, action, actor, pageable);
	}
}
