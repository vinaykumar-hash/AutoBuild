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
@Table(name = "deployment_rollback_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentRollbackLog {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "rollback_id", nullable = false)
	private UUID rollbackId;

	@Column(nullable = false, length = 2000)
	private String message;

	@Column(nullable = false)
	private Instant timestamp;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DeploymentLogLevel level;
}
