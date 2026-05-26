package com.pipelineforge.deployment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "deployment_rollback_points")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentRollbackPoint {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "deployment_id", nullable = false)
	private UUID deploymentId;

	@Column(name = "stage_name", nullable = false, length = 32)
	private String stageName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DeploymentRollbackStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(length = 2000)
	private String metadata;
}
