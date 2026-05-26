package com.pipelineforge.repositorymgmt.validation;

import com.pipelineforge.repositorymgmt.dto.RepositoryUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class RepositoryValidation {
	public static final String GITHUB_URL_REGEX = "^https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(\\.git)?$";

	private RepositoryValidation() {
	}

	public static void validateGithubUrl(String githubUrl) {
		if (githubUrl == null || !githubUrl.matches(GITHUB_URL_REGEX)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GitHub repository URL");
		}
	}

	public static void validateUpdate(RepositoryUpdateRequest request) {
		validateOptional(request.repoName(), "repoName");
		validateOptional(request.branch(), "branch");
		validateOptional(request.accessToken(), "accessToken");
		validateOptional(request.githubUrl(), "githubUrl");
	}

	private static void validateOptional(String value, String fieldName) {
		if (value != null && value.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must not be blank");
		}
	}
}
