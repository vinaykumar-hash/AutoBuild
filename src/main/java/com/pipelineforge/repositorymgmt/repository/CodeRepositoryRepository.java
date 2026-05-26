package com.pipelineforge.repositorymgmt.repository;

import com.pipelineforge.repositorymgmt.entity.CodeRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CodeRepositoryRepository extends JpaRepository<CodeRepository, UUID>, JpaSpecificationExecutor<CodeRepository> {
	boolean existsByGithubUrlAndBranch(String githubUrl, String branch);

	Optional<CodeRepository> findByGithubUrl(String githubUrl);

	Optional<CodeRepository> findByGithubUrlAndBranch(String githubUrl, String branch);
}
