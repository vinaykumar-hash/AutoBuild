package com.pipelineforge.pipeline.validation;

import com.pipelineforge.pipeline.dto.PipelineStageRequest;
import com.pipelineforge.pipeline.entity.PipelineStageName;
import com.pipelineforge.pipeline.entity.PipelineStatus;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PipelineValidation {
	private PipelineValidation() {
	}

	public static void validateStages(List<PipelineStageRequest> stages) {
		Set<Integer> orderSet = new HashSet<>();
		Set<PipelineStageName> nameSet = new HashSet<>();
		for (PipelineStageRequest stage : stages) {
			if (!orderSet.add(stage.executionOrder())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stage executionOrder must be unique");
			}
			if (!nameSet.add(stage.stageName())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stage names must be unique");
			}
		}
	}

	public static PipelineStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return PipelineStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pipeline status");
		}
	}
}
