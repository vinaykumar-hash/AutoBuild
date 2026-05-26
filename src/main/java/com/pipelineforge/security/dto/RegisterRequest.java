package com.pipelineforge.security.dto;

import com.pipelineforge.security.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "name is required")
		@Size(max = 140, message = "name must be 140 characters or fewer")
		String name,
		@NotBlank(message = "email is required")
		@Email(message = "email must be valid")
		@Size(max = 180, message = "email must be 180 characters or fewer")
		String email,
		@NotBlank(message = "password is required")
		@Size(min = 8, max = 200, message = "password must be 8-200 characters")
		String password,
		@NotNull(message = "role is required")
		Role role
) {
}
