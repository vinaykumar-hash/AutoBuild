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
@Table(name = "deployment_rollbacks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentRollbackExecution extends AuditableEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "pipeline_id", nullable = false)
	private UUID pipelineId;

	@Column(name = "from_deployment_id", nullable = false)
	private UUID fromDeploymentId;

	@Column(name = "to_deployment_id", nullable = false)
	private UUID toDeploymentId;

	@Column(name = "triggered_by", nullable = false, length = 180)
	private String triggeredBy;

	@Column(length = 1000)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private DeploymentRollbackExecutionStatus status;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "error_message", length = 2000)
	private String errorMessage;
}
