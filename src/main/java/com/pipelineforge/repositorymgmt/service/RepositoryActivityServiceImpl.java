package com.pipelineforge.repositorymgmt.service;

import com.pipelineforge.repositorymgmt.dto.PagedResponse;
import com.pipelineforge.repositorymgmt.dto.RepositoryActivityResponse;
import com.pipelineforge.repositorymgmt.entity.RepositoryActivity;
import com.pipelineforge.repositorymgmt.entity.RepositoryActivityType;
import com.pipelineforge.repositorymgmt.repository.RepositoryActivityRepository;
import com.pipelineforge.repositorymgmt.repository.RepositoryActivitySpecifications;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryActivityServiceImpl implements RepositoryActivityService {
	private final RepositoryActivityRepository repositoryActivityRepository;

	@Override
	@Transactional
	public void log(UUID repositoryId, String repoName, RepositoryActivityType action, String details, String actor) {
		RepositoryActivity activity = RepositoryActivity.builder()
				.repositoryId(repositoryId)
				.repoName(repoName)
				.action(action)
				.details(details)
				.actor(actor)
				.build();
		repositoryActivityRepository.save(activity);
	}

	@Override
	@Transactional(readOnly = true)
	public PagedResponse<RepositoryActivityResponse> list(UUID repositoryId, String action, String actor, Pageable pageable) {
		Specification<RepositoryActivity> specification = Specification
				.where(RepositoryActivitySpecifications.repositoryIdEquals(repositoryId))
				.and(RepositoryActivitySpecifications.actionEquals(action))
				.and(RepositoryActivitySpecifications.actorEquals(actor));
		Page<RepositoryActivityResponse> page = repositoryActivityRepository.findAll(specification, pageable)
				.map(RepositoryActivityResponse::from);
		return new PagedResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
		);
	}
}
