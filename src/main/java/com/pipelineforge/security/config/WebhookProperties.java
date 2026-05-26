package com.pipelineforge.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.webhook")
public record WebhookProperties(String githubSecret) {
}
