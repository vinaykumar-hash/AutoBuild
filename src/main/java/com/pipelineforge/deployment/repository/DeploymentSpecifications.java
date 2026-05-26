package com.pipelineforge.deployment.repository;

import com.pipelineforge.deployment.entity.Deployment;
import com.pipelineforge.deployment.entity.DeploymentStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class DeploymentSpecifications {
	private DeploymentSpecifications() {
	}

	public static Specification<Deployment> pipelineIdEquals(UUID pipelineId) {
		return (root, query, cb) -> pipelineId == null
				? cb.conjunction()
				: cb.equal(root.get("pipelineId"), pipelineId);
	}

	public static Specification<Deployment> statusEquals(String status) {
		return (root, query, cb) -> Optional.ofNullable(status)
				.filter(value -> !value.isBlank())
				.map(value -> cb.equal(root.get("status"), DeploymentStatus.valueOf(value.toUpperCase())))
				.orElseGet(cb::conjunction);
	}

	public static Specification<Deployment> currentStageEquals(String stage) {
		return (root, query, cb) -> Optional.ofNullable(stage)
				.filter(value -> !value.isBlank())
				.map(value -> cb.equal(cb.lower(root.get("currentStage")), value.toLowerCase()))
				.orElseGet(cb::conjunction);
	}

	public static Specification<Deployment> startedAtBetween(Instant from, Instant to) {
		return (root, query, cb) -> {
			if (from == null && to == null) {
				return cb.conjunction();
			}
			if (from != null && to != null) {
				return cb.between(root.get("startedAt"), from, to);
			}
			return from != null
					? cb.greaterThanOrEqualTo(root.get("startedAt"), from)
					: cb.lessThanOrEqualTo(root.get("startedAt"), to);
		};
	}
}
