package com.pipelineforge.repositorymgmt.repository;

import com.pipelineforge.repositorymgmt.entity.CodeRepository;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

public final class RepositorySpecifications {
	private RepositorySpecifications() {
	}

	public static Specification<CodeRepository> repoNameContains(String repoName) {
		return (root, query, cb) -> Optional.ofNullable(repoName)
				.filter(value -> !value.isBlank())
				.map(value -> cb.like(cb.lower(root.get("repoName")), "%" + value.toLowerCase() + "%"))
				.orElseGet(cb::conjunction);
	}

	public static Specification<CodeRepository> branchEquals(String branch) {
		return (root, query, cb) -> Optional.ofNullable(branch)
				.filter(value -> !value.isBlank())
				.map(value -> cb.equal(root.get("branch"), value))
				.orElseGet(cb::conjunction);
	}

	public static Specification<CodeRepository> createdByEquals(String createdBy) {
		return (root, query, cb) -> Optional.ofNullable(createdBy)
				.filter(value -> !value.isBlank())
				.map(value -> cb.equal(cb.lower(root.get("createdBy")), value.toLowerCase()))
				.orElseGet(cb::conjunction);
	}
}
