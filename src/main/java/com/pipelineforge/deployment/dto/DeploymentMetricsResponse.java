package com.pipelineforge.deployment.dto;

public record DeploymentMetricsResponse(
		long totalDeployments,
		long successfulDeployments,
		long failedDeployments,
		double failureRate,
		double averageDurationMs
) {
}
