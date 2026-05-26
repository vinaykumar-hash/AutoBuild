package com.pipelineforge.deployment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deployment.runner")
public record DeploymentRunnerProperties(
		String containerName,
		String healthCheckUrl,
		int startupWaitSeconds
) {
	public DeploymentRunnerProperties {
		if (containerName == null || containerName.isBlank()) {
			containerName = "pipelineforge-runner";
		}
		if (healthCheckUrl == null || healthCheckUrl.isBlank()) {
			healthCheckUrl = "http://localhost:3000";
		}
		if (startupWaitSeconds <= 0) {
			startupWaitSeconds = 5;
		}
	}
}
