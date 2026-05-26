package com.pipelineforge.pipeline.repository;

import com.pipelineforge.pipeline.entity.Pipeline;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PipelineRepository extends JpaRepository<Pipeline, UUID>, JpaSpecificationExecutor<Pipeline> {
	List<Pipeline> findByRepositoryId(UUID repositoryId);
}
