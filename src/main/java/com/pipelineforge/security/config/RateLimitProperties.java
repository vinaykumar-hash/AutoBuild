package com.pipelineforge.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.ratelimit")
public record RateLimitProperties(
		boolean enabled,
		int limit,
		long durationSeconds
) {
}
