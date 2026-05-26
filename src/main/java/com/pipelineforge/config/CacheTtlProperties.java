package com.pipelineforge.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cache.ttl")
public record CacheTtlProperties(
		Duration pipelineStatus,
		Duration deploymentSummary,
		Duration repository
) {
}
