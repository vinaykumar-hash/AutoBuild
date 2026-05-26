package com.pipelineforge.deployment.service;

import com.pipelineforge.deployment.entity.DeploymentLog;
import com.pipelineforge.deployment.entity.DeploymentLogLevel;
import com.pipelineforge.deployment.repository.DeploymentLogRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeploymentLogServiceImpl implements DeploymentLogService {
	private final DeploymentLogRepository deploymentLogRepository;

	@Override
	@Transactional
	public void append(UUID deploymentId, String message, DeploymentLogLevel level) {
		DeploymentLog log = DeploymentLog.builder()
				.deploymentId(deploymentId)
				.message(message)
				.timestamp(Instant.now())
				.level(level)
				.build();
		deploymentLogRepository.save(log);
	}
}
