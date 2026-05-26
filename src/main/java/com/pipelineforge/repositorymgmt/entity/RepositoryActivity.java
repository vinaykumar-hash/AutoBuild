package com.pipelineforge.repositorymgmt.entity;

import com.pipelineforge.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "repository_activity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryActivity extends AuditableEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "repository_id", nullable = false)
	private UUID repositoryId;

	@Column(name = "repo_name", nullable = false, length = 140)
	private String repoName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private RepositoryActivityType action;

	@Column(length = 2000)
	private String details;

	@Column(nullable = false, length = 180)
	private String actor;
}
