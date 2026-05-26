package com.pipelineforge.deployment.service;

import com.pipelineforge.config.CacheNames;
import com.pipelineforge.deployment.dto.DeploymentJobEvent;
import com.pipelineforge.deployment.dto.DeploymentLogResponse;
import com.pipelineforge.deployment.dto.DeploymentMetricsResponse;
import com.pipelineforge.deployment.dto.DeploymentResponse;
import com.pipelineforge.deployment.dto.PagedResponse;
import com.pipelineforge.deployment.entity.Deployment;
import com.pipelineforge.deployment.entity.DeploymentLogLevel;
import com.pipelineforge.deployment.entity.DeploymentStatus;
import com.pipelineforge.deployment.repository.DeploymentLogRepository;
import com.pipelineforge.deployment.repository.DeploymentRepository;
import com.pipelineforge.deployment.repository.DeploymentSpecifications;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DeploymentServiceImpl implements DeploymentService {
	private final DeploymentRepository deploymentRepository;
	private final DeploymentLogRepository deploymentLogRepository;
	private final DeploymentLogService deploymentLogService;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheNames.DEPLOYMENT_SUMMARY, key = "#id")
	public DeploymentResponse get(UUID id) {
		Deployment deployment = deploymentRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
		return DeploymentResponse.from(deployment);
	}

	@Override
	@Transactional(readOnly = true)
	public PagedResponse<DeploymentResponse> list(UUID pipelineId, String status, String stage, Instant from, Instant to, Pageable pageable) {
		Specification<Deployment> specification = Specification
				.where(DeploymentSpecifications.pipelineIdEquals(pipelineId))
				.and(DeploymentSpecifications.statusEquals(status))
				.and(DeploymentSpecifications.currentStageEquals(stage))
				.and(DeploymentSpecifications.startedAtBetween(from, to));
		Page<DeploymentResponse> page = deploymentRepository.findAll(specification, pageable)
				.map(DeploymentResponse::from);
		return new PagedResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public PagedResponse<DeploymentLogResponse> logs(UUID deploymentId, Pageable pageable) {
		Page<DeploymentLogResponse> page = deploymentLogRepository.findByDeploymentId(deploymentId, pageable)
				.map(DeploymentLogResponse::from);
		return new PagedResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public DeploymentMetricsResponse metrics(Instant from, Instant to) {
		long total;
		long failed;
		long success;
		Double avgDuration;

		if (from == null && to == null) {
			total = deploymentRepository.count();
			failed = deploymentRepository.countByStatus(DeploymentStatus.FAILED);
			success = deploymentRepository.countByStatus(DeploymentStatus.SUCCESS);
			avgDuration = deploymentRepository.averageDurationMs();
		} else {
			total = deploymentRepository.countAllBetween(from, to);
			failed = deploymentRepository.countByStatusAndStartedAtBetween(DeploymentStatus.FAILED, from, to);
			success = deploymentRepository.countByStatusAndStartedAtBetween(DeploymentStatus.SUCCESS, from, to);
			avgDuration = deploymentRepository.averageDurationMsBetween(from, to);
		}

		double failureRate = total == 0 ? 0.0 : (double) failed / (double) total;
		double avgValue = avgDuration == null ? 0.0 : avgDuration;
		return new DeploymentMetricsResponse(total, success, failed, failureRate, avgValue);
	}

	@Override
	@Transactional
	public UUID startDeployment(DeploymentJobEvent event) {
		Instant now = Instant.now();
		long versionNumber = deploymentRepository.maxVersionNumber(event.pipelineId()) + 1;
		Deployment deployment = Deployment.builder()
				.pipelineId(event.pipelineId())
				.commitHash(event.commitHash())
				.versionNumber(versionNumber)
				.stable(false)
				.status(DeploymentStatus.RUNNING)
				.currentStage(null)
				.startedAt(now)
				.logs("Deployment started")
				.build();
		Deployment saved = deploymentRepository.save(deployment);
		deploymentLogService.append(saved.getId(), "Deployment started", DeploymentLogLevel.INFO);
		return saved.getId();
	}

	@Override
	@Transactional
	@CacheEvict(cacheNames = CacheNames.DEPLOYMENT_SUMMARY, key = "#deploymentId")
	public void markSuccess(UUID deploymentId) {
		Deployment deployment = deploymentRepository.findById(deploymentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
		Instant now = Instant.now();
		deployment.setStatus(DeploymentStatus.SUCCESS);
		deployment.setCompletedAt(now);
		deployment.setDurationMs(Duration.between(deployment.getStartedAt(), now).toMillis());
		deployment.setCurrentStage(null);
		deploymentRepository.findFirstByPipelineIdAndStableTrue(deployment.getPipelineId())
				.filter(existing -> !existing.getId().equals(deployment.getId()))
				.ifPresent(existing -> existing.setStable(false));
		deployment.setStable(true);
		deploymentLogService.append(deployment.getId(), "Deployment completed", DeploymentLogLevel.INFO);
	}

	@Override
	@Transactional
	@CacheEvict(cacheNames = CacheNames.DEPLOYMENT_SUMMARY, key = "#deploymentId")
	public void markFailed(UUID deploymentId, String errorMessage) {
		Deployment deployment = deploymentRepository.findById(deploymentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
		Instant now = Instant.now();
		deployment.setStatus(DeploymentStatus.FAILED);
		deployment.setCompletedAt(now);
		deployment.setDurationMs(Duration.between(deployment.getStartedAt(), now).toMillis());
		deployment.setCurrentStage(null);
		deploymentLogService.append(deployment.getId(), "Deployment failed: " + errorMessage, DeploymentLogLevel.ERROR);
	}
}
