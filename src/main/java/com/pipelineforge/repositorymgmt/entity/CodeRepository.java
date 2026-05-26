package com.pipelineforge.repositorymgmt.entity;

import com.pipelineforge.audit.AuditableEntity;
import com.pipelineforge.security.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "repositories",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_repo_github_branch",
				columnNames = {"github_url", "branch"}
		)
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeRepository extends AuditableEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "repo_name", nullable = false, length = 140)
	private String repoName;

	@Column(name = "github_url", nullable = false, length = 320)
	private String githubUrl;

	@Column(nullable = false, length = 120)
	private String branch;

	@Column(name = "encrypted_access_token", nullable = false, length = 2048)
	@Convert(converter = EncryptedStringConverter.class)
	private String encryptedAccessToken;

	@Column(name = "created_by", nullable = false, length = 180)
	private String createdBy;
}
