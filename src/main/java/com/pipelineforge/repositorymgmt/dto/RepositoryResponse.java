package com.pipelineforge.repositorymgmt.dto;

import com.pipelineforge.repositorymgmt.entity.CodeRepository;
import java.time.Instant;
import java.util.UUID;

public record RepositoryResponse(
		UUID id,
		String repoName,
		String githubUrl,
		String branch,
		Instant createdAt,
		String createdBy
) {
	public static RepositoryResponse from(CodeRepository repository) {
		return new RepositoryResponse(
				repository.getId(),
				repository.getRepoName(),
				repository.getGithubUrl(),
				repository.getBranch(),
				repository.getCreatedAt(),
				repository.getCreatedBy()
		);
	}
}
