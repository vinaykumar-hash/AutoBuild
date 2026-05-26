package com.pipelineforge.deployment.service;

import com.pipelineforge.deployment.dto.DeploymentJobEvent;

public interface DeploymentJobService {
	void process(DeploymentJobEvent event);
}
