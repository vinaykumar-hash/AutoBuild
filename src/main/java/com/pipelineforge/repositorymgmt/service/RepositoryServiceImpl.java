package com.pipelineforge.repositorymgmt.service;

import com.pipelineforge.config.CacheNames;
import com.pipelineforge.repositorymgmt.dto.PagedResponse;
import com.pipelineforge.repositorymgmt.dto.RepositoryCreateRequest;
import com.pipelineforge.repositorymgmt.dto.RepositoryResponse;
import com.pipelineforge.repositorymgmt.dto.RepositoryUpdateRequest;
import com.pipelineforge.repositorymgmt.entity.CodeRepository;
import com.pipelineforge.repositorymgmt.entity.RepositoryActivityType;
import com.pipelineforge.repositorymgmt.repository.CodeRepositoryRepository;
import com.pipelineforge.repositorymgmt.repository.RepositorySpecifications;
import com.pipelineforge.repositorymgmt.validation.RepositoryValidation;
import com.pipelineforge.security.util.SecurityContextUtils;
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
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RepositoryServiceImpl implements RepositoryService {
	private final CodeRepositoryRepository repositoryRepository;
	private final RepositoryActivityService activityService;

	@Override
	@Transactional
	@CachePut(cacheNames = CacheNames.REPOSITORY, key = "#result.id")
	public RepositoryResponse create(RepositoryCreateRequest request) {
		RepositoryValidation.validateGithubUrl(request.githubUrl());
		if (repositoryRepository.existsByGithubUrlAndBranch(request.githubUrl(), request.branch())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Repository already registered for this branch");
		}

		String actor = SecurityContextUtils.currentUsername();
		CodeRepository repository = CodeRepository.builder()
				.repoName(request.repoName())
				.githubUrl(request.githubUrl())
				.branch(request.branch())
				.encryptedAccessToken(request.accessToken())
				.createdBy(actor)
				.build();
		CodeRepository saved = repositoryRepository.save(repository);
		activityService.log(saved.getId(), saved.getRepoName(), RepositoryActivityType.CREATED, "Repository created", actor);
		return RepositoryResponse.from(saved);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheNames.REPOSITORY, key = "#id")
	public RepositoryResponse get(UUID id) {
		CodeRepository repository = repositoryRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
		return RepositoryResponse.from(repository);
	}

	@Override
	@Transactional(readOnly = true)
	public PagedResponse<RepositoryResponse> list(String repoName, String branch, String createdBy, Pageable pageable) {
		Specification<CodeRepository> specification = Specification
				.where(RepositorySpecifications.repoNameContains(repoName))
				.and(RepositorySpecifications.branchEquals(branch))
				.and(RepositorySpecifications.createdByEquals(createdBy));
		Page<RepositoryResponse> page = repositoryRepository.findAll(specification, pageable)
				.map(RepositoryResponse::from);
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
	@CachePut(cacheNames = CacheNames.REPOSITORY, key = "#id")
	public RepositoryResponse update(UUID id, RepositoryUpdateRequest request) {
		CodeRepository repository = repositoryRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));

		RepositoryValidation.validateUpdate(request);
		String nextGithubUrl = request.githubUrl() != null ? request.githubUrl() : repository.getGithubUrl();
		String nextBranch = request.branch() != null ? request.branch() : repository.getBranch();
		RepositoryValidation.validateGithubUrl(nextGithubUrl);
		repositoryRepository.findByGithubUrlAndBranch(nextGithubUrl, nextBranch)
				.filter(existing -> !existing.getId().equals(repository.getId()))
				.ifPresent(existing -> {
					throw new ResponseStatusException(HttpStatus.CONFLICT, "Repository already registered for this branch");
				});

		if (request.githubUrl() != null) {
			repository.setGithubUrl(request.githubUrl());
		}
		if (request.repoName() != null) {
			repository.setRepoName(request.repoName());
		}
		if (request.branch() != null) {
			repository.setBranch(request.branch());
		}
		if (request.accessToken() != null) {
			repository.setEncryptedAccessToken(request.accessToken());
		}

		CodeRepository saved = repositoryRepository.save(repository);
		String actor = SecurityContextUtils.currentUsername();
		activityService.log(saved.getId(), saved.getRepoName(), RepositoryActivityType.UPDATED, "Repository updated", actor);
		return RepositoryResponse.from(saved);
	}

	@Override
	@Transactional
	@CacheEvict(cacheNames = CacheNames.REPOSITORY, key = "#id")
	public void delete(UUID id) {
		CodeRepository repository = repositoryRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
		String actor = SecurityContextUtils.currentUsername();
		activityService.log(repository.getId(), repository.getRepoName(), RepositoryActivityType.DELETED, "Repository deleted", actor);
		repositoryRepository.delete(repository);
	}
}
