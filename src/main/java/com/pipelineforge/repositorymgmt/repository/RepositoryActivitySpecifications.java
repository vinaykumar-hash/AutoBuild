package com.pipelineforge.repositorymgmt.repository;

import com.pipelineforge.repositorymgmt.entity.RepositoryActivity;
import com.pipelineforge.repositorymgmt.entity.RepositoryActivityType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class RepositoryActivitySpecifications {
	private RepositoryActivitySpecifications() {
	}

	public static Specification<RepositoryActivity> repositoryIdEquals(UUID repositoryId) {
		return (root, query, cb) -> repositoryId == null
				? cb.conjunction()
				: cb.equal(root.get("repositoryId"), repositoryId);
	}

	public static Specification<RepositoryActivity> actionEquals(String action) {
		return (root, query, cb) -> Optional.ofNullable(action)
				.filter(value -> !value.isBlank())
				.map(value -> cb.equal(root.get("action"), RepositoryActivityType.valueOf(value)))
				.orElseGet(cb::conjunction);
	}

	public static Specification<RepositoryActivity> actorEquals(String actor) {
		return (root, query, cb) -> Optional.ofNullable(actor)
				.filter(value -> !value.isBlank())
				.map(value -> cb.equal(cb.lower(root.get("actor")), value.toLowerCase()))
				.orElseGet(cb::conjunction);
	}
}
