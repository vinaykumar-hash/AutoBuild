package com.pipelineforge.deployment.service;

import com.pipelineforge.config.CacheService;
import com.pipelineforge.deployment.dto.PagedResponse;
import com.pipelineforge.deployment.dto.RollbackLogResponse;
import com.pipelineforge.deployment.dto.RollbackRequest;
import com.pipelineforge.deployment.dto.RollbackResponse;
import com.pipelineforge.deployment.entity.Deployment;
import com.pipelineforge.deployment.entity.DeploymentLogLevel;
import com.pipelineforge.deployment.entity.DeploymentRollbackExecution;
import com.pipelineforge.deployment.entity.DeploymentRollbackExecutionStatus;
import com.pipelineforge.deployment.entity.DeploymentRollbackLog;
import com.pipelineforge.deployment.entity.DeploymentStatus;
import com.pipelineforge.deployment.repository.DeploymentRepository;
import com.pipelineforge.deployment.repository.DeploymentRollbackExecutionRepository;
import com.pipelineforge.deployment.repository.DeploymentRollbackLogRepository;
import com.pipelineforge.pipeline.entity.Pipeline;
import com.pipelineforge.pipeline.entity.PipelineStatus;
import com.pipelineforge.pipeline.repository.PipelineRepository;
import com.pipelineforge.security.util.SecurityContextUtils;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RollbackServiceImpl implements RollbackService {
	private final DeploymentRepository deploymentRepository;
	private final DeploymentRollbackExecutionRepository rollbackExecutionRepository;
	private final DeploymentRollbackLogRepository rollbackLogRepository;
	private final PipelineRepository pipelineRepository;
	private final CacheService cacheService;

	@Override
	@Transactional
	public RollbackResponse rollback(RollbackRequest request) {
		Pipeline pipeline = pipelineRepository.findById(request.pipelineId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pipeline not found"));
		if (rollbackExecutionRepository.existsByPipelineIdAndStatus(request.pipelineId(), DeploymentRollbackExecutionStatus.RUNNING)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Rollback already in progress");
		}

		Deployment current = deploymentRepository.findTopByPipelineIdOrderByVersionNumberDesc(request.pipelineId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No deployments to roll back"));
		if (current.getStatus() == DeploymentStatus.RUNNING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot rollback a running deployment");
		}

		Deployment target = deploymentRepository.findById(request.targetDeploymentId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target deployment not found"));
		if (!target.getPipelineId().equals(request.pipelineId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target deployment does not belong to pipeline");
		}
		if (target.getStatus() != DeploymentStatus.SUCCESS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target deployment is not successful");
		}
		if (target.getVersionNumber() >= current.getVersionNumber()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target deployment must be older than current");
		}

		String actor = SecurityContextUtils.currentUsername();
		DeploymentRollbackExecution rollback = rollbackExecutionRepository.save(DeploymentRollbackExecution.builder()
				.pipelineId(request.pipelineId())
				.fromDeploymentId(current.getId())
				.toDeploymentId(target.getId())
				.triggeredBy(actor)
				.reason(request.reason())
				.status(DeploymentRollbackExecutionStatus.RUNNING)
				.startedAt(Instant.now())
				.build());
		appendLog(rollback.getId(), "Rollback started by " + actor, DeploymentLogLevel.INFO);

		try {
			pipeline.setStatus(PipelineStatus.RUNNING);
			pipeline.setLastExecutionStartedAt(Instant.now());
			current.setStatus(DeploymentStatus.ROLLED_BACK);
			current.setStable(false);

			deploymentRepository.findFirstByPipelineIdAndStableTrue(request.pipelineId())
					.filter(existing -> !existing.getId().equals(target.getId()))
					.ifPresent(existing -> existing.setStable(false));
			target.setStable(true);

			pipeline.setStatus(PipelineStatus.SUCCESS);
			pipeline.setLastExecutionFinishedAt(Instant.now());
			rollback.setStatus(DeploymentRollbackExecutionStatus.SUCCESS);
			rollback.setCompletedAt(Instant.now());
			appendLog(rollback.getId(), "Rollback completed to deployment " + target.getId(), DeploymentLogLevel.INFO);
			cacheService.evictPipelineStatus(pipeline.getId());
			cacheService.evictDeploymentSummary(current.getId());
			cacheService.evictDeploymentSummary(target.getId());
		} catch (Exception ex) {
			rollback.setStatus(DeploymentRollbackExecutionStatus.FAILED);
			rollback.setCompletedAt(Instant.now());
			rollback.setErrorMessage(ex.getMessage());
			pipeline.setStatus(PipelineStatus.FAILED);
			pipeline.setLastExecutionFinishedAt(Instant.now());
			appendLog(rollback.getId(), "Rollback failed: " + ex.getMessage(), DeploymentLogLevel.ERROR);
			cacheService.evictPipelineStatus(pipeline.getId());
			throw ex;
		}

		return RollbackResponse.from(rollback, current.getVersionNumber(), target.getVersionNumber());
	}

	@Override
	@Transactional(readOnly = true)
	public PagedResponse<RollbackResponse> history(UUID pipelineId, Pageable pageable) {
		Page<DeploymentRollbackExecution> page = pipelineId == null
				? rollbackExecutionRepository.findAll(pageable)
				: rollbackExecutionRepository.findByPipelineId(pipelineId, pageable);
		java.util.Map<UUID, Long> versionMap = deploymentRepository.findAllById(
				page.stream()
						.flatMap(rollback -> java.util.stream.Stream.of(rollback.getFromDeploymentId(), rollback.getToDeploymentId()))
						.distinct()
						.toList())
				.stream()
				.collect(java.util.stream.Collectors.toMap(Deployment::getId, Deployment::getVersionNumber));
		return new PagedResponse<>(
				page.map(rollback -> RollbackResponse.from(
						rollback,
						versionMap.getOrDefault(rollback.getFromDeploymentId(), 0L),
						versionMap.getOrDefault(rollback.getToDeploymentId(), 0L)
				)).getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public PagedResponse<RollbackLogResponse> logs(UUID rollbackId, Pageable pageable) {
		Page<RollbackLogResponse> page = rollbackLogRepository.findByRollbackId(rollbackId, pageable)
				.map(RollbackLogResponse::from);
		return new PagedResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
		);
	}

	private void appendLog(UUID rollbackId, String message, DeploymentLogLevel level) {
		rollbackLogRepository.save(DeploymentRollbackLog.builder()
				.rollbackId(rollbackId)
				.message(message)
				.level(level)
				.timestamp(Instant.now())
				.build());
	}
}
