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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "deployment_job_executions", uniqueConstraints = @UniqueConstraint(name = "uk_deployment_message", columnNames = "message_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentJobExecution extends AuditableEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "message_id", nullable = false, length = 64)
	private String messageId;

	@Column(name = "pipeline_id", nullable = false)
	private UUID pipelineId;

	@Column(name = "deployment_id")
	private UUID deploymentId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private DeploymentJobStatus status;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "error_message", length = 2000)
	private String errorMessage;
}
