package com.pipelineforge.pipeline.entity;

import com.pipelineforge.audit.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pipelines")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pipeline extends AuditableEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "pipeline_name", nullable = false, length = 140)
	private String pipelineName;

	@Column(name = "repository_id", nullable = false)
	private UUID repositoryId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private PipelineStatus status;

	@Column(name = "created_by", nullable = false, length = 180)
	private String createdBy;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "last_triggered_at")
	private Instant lastTriggeredAt;

	@Column(name = "last_execution_started_at")
	private Instant lastExecutionStartedAt;

	@Column(name = "last_execution_finished_at")
	private Instant lastExecutionFinishedAt;

	@OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("executionOrder ASC")
	@Builder.Default
	private List<PipelineStage> stages = new ArrayList<>();
}
