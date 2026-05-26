package com.pipelineforge.repositorymgmt.service;

import com.pipelineforge.repositorymgmt.dto.PagedResponse;
import com.pipelineforge.repositorymgmt.dto.RepositoryCreateRequest;
import com.pipelineforge.repositorymgmt.dto.RepositoryResponse;
import com.pipelineforge.repositorymgmt.dto.RepositoryUpdateRequest;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface RepositoryService {
	RepositoryResponse create(RepositoryCreateRequest request);

	RepositoryResponse get(UUID id);

	PagedResponse<RepositoryResponse> list(String repoName, String branch, String createdBy, Pageable pageable);

	RepositoryResponse update(UUID id, RepositoryUpdateRequest request);

	void delete(UUID id);
}
