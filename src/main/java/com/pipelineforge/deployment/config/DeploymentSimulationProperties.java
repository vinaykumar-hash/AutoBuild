package com.pipelineforge.deployment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deployment.simulation")
public record DeploymentSimulationProperties(
		String failStage,
		double failureRate
) {
}
