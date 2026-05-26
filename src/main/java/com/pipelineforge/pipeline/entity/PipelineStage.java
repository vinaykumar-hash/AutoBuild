package com.pipelineforge.pipeline.entity;

import com.pipelineforge.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pipeline_stages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStage extends AuditableEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pipeline_id", nullable = false)
	private Pipeline pipeline;

	@Enumerated(EnumType.STRING)
	@Column(name = "stage_name", nullable = false, length = 32)
	private PipelineStageName stageName;

	@Column(name = "execution_order", nullable = false)
	private int executionOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private PipelineStageStatus status;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;
}
