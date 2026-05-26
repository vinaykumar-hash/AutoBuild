package com.pipelineforge.repositorymgmt.dto;

import com.pipelineforge.repositorymgmt.entity.RepositoryActivity;
import com.pipelineforge.repositorymgmt.entity.RepositoryActivityType;
import java.time.Instant;
import java.util.UUID;

public record RepositoryActivityResponse(
		UUID id,
		UUID repositoryId,
		String repoName,
		RepositoryActivityType action,
		String details,
		String actor,
		Instant createdAt
) {
	public static RepositoryActivityResponse from(RepositoryActivity activity) {
		return new RepositoryActivityResponse(
				activity.getId(),
				activity.getRepositoryId(),
				activity.getRepoName(),
				activity.getAction(),
				activity.getDetails(),
				activity.getActor(),
				activity.getCreatedAt()
		);
	}
}
