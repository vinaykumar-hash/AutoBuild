package com.pipelineforge.pipeline.repository;

import com.pipelineforge.pipeline.entity.Pipeline;
import com.pipelineforge.pipeline.entity.PipelineStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class PipelineSpecifications {
	private PipelineSpecifications() {
	}

	public static Specification<Pipeline> statusEquals(PipelineStatus status) {
		return (root, query, cb) -> status == null
				? cb.conjunction()
				: cb.equal(root.get("status"), status);
	}

	public static Specification<Pipeline> repositoryIdEquals(UUID repositoryId) {
		return (root, query, cb) -> repositoryId == null
				? cb.conjunction()
				: cb.equal(root.get("repositoryId"), repositoryId);
	}

	public static Specification<Pipeline> createdByEquals(String createdBy) {
		return (root, query, cb) -> Optional.ofNullable(createdBy)
				.filter(value -> !value.isBlank())
				.map(value -> cb.equal(cb.lower(root.get("createdBy")), value.toLowerCase()))
				.orElseGet(cb::conjunction);
	}
}
