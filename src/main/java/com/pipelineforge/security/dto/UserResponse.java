package com.pipelineforge.security.dto;

import com.pipelineforge.security.entity.Role;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
		UUID id,
		String name,
		String email,
		Role role,
		Instant createdAt
) {
}
