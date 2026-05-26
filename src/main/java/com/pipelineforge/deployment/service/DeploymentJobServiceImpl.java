package com.pipelineforge.deployment.service;

import com.pipelineforge.deployment.dto.DeploymentJobEvent;
import com.pipelineforge.deployment.entity.DeploymentJobExecution;
import com.pipelineforge.deployment.entity.DeploymentJobStatus;
import com.pipelineforge.deployment.engine.DeploymentExecutionEngine;
import com.pipelineforge.deployment.repository.DeploymentJobExecutionRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeploymentJobServiceImpl implements DeploymentJobService {
	private static final Logger logger = LoggerFactory.getLogger(DeploymentJobServiceImpl.class);

	private final DeploymentJobExecutionRepository executionRepository;
	private final DeploymentExecutionEngine executionEngine;

	@Override
	public void process(DeploymentJobEvent event) {
		DeploymentJobExecution execution = executionRepository.findByMessageId(event.messageId())
				.orElseGet(() -> createExecution(event));
		if (execution.getStatus() == DeploymentJobStatus.SUCCESS) {
			logger.info("Skipping already completed deployment messageId={}", event.messageId());
			return;
		}
		if (execution.getStatus() == DeploymentJobStatus.RUNNING) {
			logger.info("Skipping in-progress deployment messageId={}", event.messageId());
			return;
		}

		try {
			Instant now = Instant.now();
			execution.setStatus(DeploymentJobStatus.RUNNING);
			execution.setAttempts(execution.getAttempts() + 1);
			execution.setStartedAt(now);
			execution.setErrorMessage(null);
			executionRepository.save(execution);
			logger.info("Dispatching deployment for pipelineId={} messageId={}", event.pipelineId(), event.messageId());
			executionEngine.executeAsync(event);
		} catch (Exception ex) {
			execution.setStatus(DeploymentJobStatus.FAILED);
			execution.setFinishedAt(Instant.now());
			execution.setErrorMessage(ex.getMessage());
			executionRepository.save(execution);
			logger.error("Deployment failed for pipelineId={} messageId={} error={}", event.pipelineId(), event.messageId(), ex.getMessage());
			throw ex;
		}
	}

	private DeploymentJobExecution createExecution(DeploymentJobEvent event) {
		try {
			return executionRepository.save(DeploymentJobExecution.builder()
					.messageId(event.messageId())
					.pipelineId(event.pipelineId())
					.status(DeploymentJobStatus.PENDING)
					.attempts(0)
					.build());
		} catch (DataIntegrityViolationException ex) {
			return executionRepository.findByMessageId(event.messageId())
					.orElseThrow(() -> new IllegalStateException("Duplicate message in progress"));
		}
	}
}
