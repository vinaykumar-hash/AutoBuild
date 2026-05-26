package com.pipelineforge.repositorymgmt.dto;

import com.pipelineforge.repositorymgmt.validation.RepositoryValidation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RepositoryCreateRequest(
		@NotBlank(message = "repoName is required")
		@Size(max = 140, message = "repoName must be 140 characters or fewer")
		String repoName,
		@NotBlank(message = "githubUrl is required")
		@Pattern(regexp = RepositoryValidation.GITHUB_URL_REGEX, message = "githubUrl must be a valid GitHub repository URL")
		String githubUrl,
		@NotBlank(message = "branch is required")
		@Size(max = 120, message = "branch must be 120 characters or fewer")
		String branch,
		@NotBlank(message = "accessToken is required")
		@Size(max = 2000, message = "accessToken must be 2000 characters or fewer")
		String accessToken
) {
}
