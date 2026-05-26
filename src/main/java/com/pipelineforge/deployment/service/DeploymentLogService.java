package com.pipelineforge.deployment.service;

import com.pipelineforge.deployment.entity.DeploymentLogLevel;

public interface DeploymentLogService {
	void append(java.util.UUID deploymentId, String message, DeploymentLogLevel level);
}
