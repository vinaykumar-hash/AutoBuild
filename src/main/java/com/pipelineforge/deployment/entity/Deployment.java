package com.pipelineforge.deployment.entity;

import com.pipelineforge.audit.AuditableEntity;
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
@Table(name = "deployments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deployment extends AuditableEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "pipeline_id", nullable = false)
	private UUID pipelineId;

	@Column(name = "commit_hash", nullable = false, length = 64)
	private String commitHash;

	@Column(name = "version_number", nullable = false)
	private long versionNumber;

	@Column(name = "stable", nullable = false)
	private boolean stable;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private DeploymentStatus status;

	@Column(name = "current_stage", length = 32)
	private String currentStage;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "duration_ms")
	private Long durationMs;

	@Column(length = 4000)
	private String logs;
}
