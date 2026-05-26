package com.pipelineforge.repositorymgmt.controller;

import com.pipelineforge.repositorymgmt.dto.GithubPushWebhookRequest;
import com.pipelineforge.repositorymgmt.entity.CodeRepository;
import com.pipelineforge.repositorymgmt.entity.RepositoryActivityType;
import com.pipelineforge.repositorymgmt.repository.CodeRepositoryRepository;
import com.pipelineforge.repositorymgmt.service.RepositoryActivityService;
import com.pipelineforge.pipeline.entity.Pipeline;
import com.pipelineforge.pipeline.repository.PipelineRepository;
import com.pipelineforge.pipeline.service.PipelineService;
import com.pipelineforge.security.config.WebhookProperties;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/repositories/webhooks")
@RequiredArgsConstructor
public class RepositoryWebhookController {
	private final CodeRepositoryRepository repositoryRepository;
	private final RepositoryActivityService activityService;
	private final WebhookProperties webhookProperties;
	private final PipelineRepository pipelineRepository;
	private final PipelineService pipelineService;

	@PostMapping("/github")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void handleGithubPush(
			@RequestHeader(name = "X-PipelineForge-Webhook-Token", required = false) String token,
			@RequestBody GithubPushWebhookRequest payload
	) {
		String expected = webhookProperties.githubSecret();
		if (expected == null || expected.isBlank()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook secret not configured");
		}
		if (token == null || !token.equals(expected)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook token");
		}

		String repoFullName = Optional.ofNullable(payload.repository())
				.map(GithubPushWebhookRequest.RepositoryInfo::fullName)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing repository info"));
		String githubUrl = "https://github.com/" + repoFullName;
		CodeRepository repository = repositoryRepository.findByGithubUrl(githubUrl)
				.orElseGet(() -> repositoryRepository.findByGithubUrl(githubUrl + ".git")
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not registered")));

		String actor = Optional.ofNullable(payload.pusher())
				.map(GithubPushWebhookRequest.PusherInfo::name)
				.orElse("github");
		String details = payload.ref() == null ? "Push event" : "Push to " + payload.ref();
		activityService.log(repository.getId(), repository.getRepoName(), RepositoryActivityType.WEBHOOK_PUSH, details, actor);

		List<Pipeline> pipelines = pipelineRepository.findByRepositoryId(repository.getId());
		if (pipelines != null) {
			for (Pipeline pipeline : pipelines) {
				pipelineService.trigger(pipeline.getId());
			}
		}
	}
}
