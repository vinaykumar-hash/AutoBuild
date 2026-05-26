package com.pipelineforge.repositorymgmt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubPushWebhookRequest(
		String ref,
		RepositoryInfo repository,
		PusherInfo pusher,
		@JsonProperty("head_commit") CommitInfo headCommit
) {
	public record RepositoryInfo(@JsonProperty("full_name") String fullName) {
	}

	public record PusherInfo(String name) {
	}

	public record CommitInfo(String id, String message) {
	}
}
