package com.pipelineforge.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.encryption")
public record EncryptionProperties(String secret) {
}
