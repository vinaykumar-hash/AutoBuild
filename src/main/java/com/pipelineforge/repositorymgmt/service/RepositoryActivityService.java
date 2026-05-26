package com.pipelineforge.repositorymgmt.service;

import com.pipelineforge.repositorymgmt.dto.PagedResponse;
import com.pipelineforge.repositorymgmt.dto.RepositoryActivityResponse;
import com.pipelineforge.repositorymgmt.entity.RepositoryActivityType;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface RepositoryActivityService {
	void log(UUID repositoryId, String repoName, RepositoryActivityType action, String details, String actor);

	PagedResponse<RepositoryActivityResponse> list(UUID repositoryId, String action, String actor, Pageable pageable);
}
