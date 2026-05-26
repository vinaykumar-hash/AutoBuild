package com.pipelineforge.deployment.repository;

import com.pipelineforge.deployment.entity.Deployment;
import com.pipelineforge.deployment.entity.DeploymentStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeploymentRepository extends JpaRepository<Deployment, UUID>, JpaSpecificationExecutor<Deployment> {
	@Query("select coalesce(max(d.versionNumber), 0) from Deployment d where d.pipelineId = :pipelineId")
	long maxVersionNumber(@Param("pipelineId") UUID pipelineId);

	java.util.Optional<Deployment> findFirstByPipelineIdAndStableTrue(UUID pipelineId);

	java.util.Optional<Deployment> findTopByPipelineIdOrderByVersionNumberDesc(UUID pipelineId);
	@Query("select count(d) from Deployment d where d.status = :status"
			+ " and (:from is null or d.startedAt >= :from)"
			+ " and (:to is null or d.startedAt <= :to)")
	long countByStatusAndStartedAtBetween(
			@Param("status") DeploymentStatus status,
			@Param("from") Instant from,
			@Param("to") Instant to
	);

	long countByStatus(DeploymentStatus status);

	@Query("select count(d) from Deployment d where (:from is null or d.startedAt >= :from)"
			+ " and (:to is null or d.startedAt <= :to)")
	long countAllBetween(
			@Param("from") Instant from,
			@Param("to") Instant to
	);

	@Query("select avg(d.durationMs) from Deployment d"
			+ " where (:from is null or d.startedAt >= :from)"
			+ " and (:to is null or d.startedAt <= :to)")
	Double averageDurationMsBetween(
			@Param("from") Instant from,
			@Param("to") Instant to
	);

	@Query("select avg(d.durationMs) from Deployment d")
	Double averageDurationMs();
}
