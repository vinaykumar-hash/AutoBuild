package com.pipelineforge.security.service;

import com.pipelineforge.security.dto.AuthResponse;
import com.pipelineforge.security.dto.LoginRequest;
import com.pipelineforge.security.dto.RegisterRequest;

public interface AuthService {
	AuthResponse register(RegisterRequest request);

	AuthResponse login(LoginRequest request);
}
