package com.pipelineforge.pipeline.service;

import com.pipelineforge.config.CacheNames;
import com.pipelineforge.deployment.dto.DeploymentJobEvent;
import com.pipelineforge.deployment.service.DeploymentJobPublisher;
import com.pipelineforge.pipeline.dto.PagedResponse;
import com.pipelineforge.pipeline.dto.PipelineCreateRequest;
import com.pipelineforge.pipeline.dto.PipelineResponse;
import com.pipelineforge.pipeline.dto.PipelineStageRequest;
import com.pipelineforge.pipeline.entity.Pipeline;
import com.pipelineforge.pipeline.entity.PipelineStage;
import com.pipelineforge.pipeline.entity.PipelineStageStatus;
import com.pipelineforge.pipeline.entity.PipelineStatus;
import com.pipelineforge.pipeline.repository.PipelineRepository;
import com.pipelineforge.pipeline.repository.PipelineSpecifications;
import com.pipelineforge.pipeline.validation.PipelineValidation;
import com.pipelineforge.repositorymgmt.repository.CodeRepositoryRepository;
import com.pipelineforge.security.util.SecurityContextUtils;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PipelineServiceImpl implements PipelineService {
	private final PipelineRepository pipelineRepository;
	private final CodeRepositoryRepository repositoryRepository;
	private final DeploymentJobPublisher deploymentJobPublisher;

	@Override
	@Transactional
	@CacheEvict(cacheNames = CacheNames.PIPELINE_STATUS, allEntries = true)
	public PipelineResponse create(PipelineCreateRequest request) {
		PipelineValidation.validateStages(request.stages());
		if (!repositoryRepository.existsById(request.repositoryId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repository not found");
		}

		String actor = SecurityContextUtils.currentUsername();
		Pipeline pipeline = Pipeline.builder()
				.pipelineName(request.pipelineName())
				.repositoryId(request.repositoryId())
				.status(PipelineStatus.PENDING)
				.createdBy(actor)
				.build();
		List<PipelineStage> stages = request.stages().stream()
				.map(stage -> toStage(stage, pipeline))
				.toList();
		pipeline.getStages().addAll(stages);
		Pipeline saved = pipelineRepository.save(pipeline);
		return PipelineResponse.from(saved);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheNames.PIPELINE_STATUS, key = "#id")
	public PipelineResponse get(UUID id) {
		Pipeline pipeline = pipelineRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pipeline not found"));
		return PipelineResponse.from(pipeline);
	}

	@Override
	@Transactional(readOnly = true)
	public PagedResponse<PipelineResponse> list(String status, UUID repositoryId, String createdBy, Pageable pageable) {
		PipelineStatus pipelineStatus = PipelineValidation.parseStatus(status);
		Specification<Pipeline> specification = Specification
				.where(PipelineSpecifications.statusEquals(pipelineStatus))
				.and(PipelineSpecifications.repositoryIdEquals(repositoryId))
				.and(PipelineSpecifications.createdByEquals(createdBy));
		Page<PipelineResponse> page = pipelineRepository.findAll(specification, pageable)
				.map(PipelineResponse::from);
		return new PagedResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
		);
	}

	@Override
	@Transactional
	@CachePut(cacheNames = CacheNames.PIPELINE_STATUS, key = "#id")
	public PipelineResponse trigger(UUID id) {
		Pipeline pipeline = pipelineRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pipeline not found"));
		Instant now = Instant.now();
		pipeline.setStatus(PipelineStatus.RUNNING);
		pipeline.setLastTriggeredAt(now);
		pipeline.setLastExecutionStartedAt(now);
		pipeline.setLastExecutionFinishedAt(null);
		pipeline.getStages().forEach(stage -> {
			stage.setStatus(PipelineStageStatus.PENDING);
			stage.setStartedAt(null);
			stage.setFinishedAt(null);
		});
		pipeline.getStages().stream()
				.min((left, right) -> Integer.compare(left.getExecutionOrder(), right.getExecutionOrder()))
				.ifPresent(stage -> {
					stage.setStatus(PipelineStageStatus.RUNNING);
					stage.setStartedAt(now);
				});
		Pipeline saved = pipelineRepository.save(pipeline);
		DeploymentJobEvent event = DeploymentJobEvent.create(saved.getId(), saved.getCreatedBy(), saved.getVersion(), null);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				deploymentJobPublisher.publish(event);
			}
		});
		return PipelineResponse.from(saved);
	}

	private PipelineStage toStage(PipelineStageRequest request, Pipeline pipeline) {
		return PipelineStage.builder()
				.pipeline(pipeline)
				.stageName(request.stageName())
				.executionOrder(request.executionOrder())
				.status(PipelineStageStatus.PENDING)
				.build();
	}
}
