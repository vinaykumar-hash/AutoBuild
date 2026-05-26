package com.pipelineforge.pipeline.repository;

import com.pipelineforge.pipeline.entity.PipelineStage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID> {
}
