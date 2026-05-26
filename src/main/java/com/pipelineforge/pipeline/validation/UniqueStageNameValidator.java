package com.pipelineforge.pipeline.validation;

import com.pipelineforge.pipeline.dto.PipelineStageRequest;
import com.pipelineforge.pipeline.entity.PipelineStageName;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UniqueStageNameValidator implements ConstraintValidator<UniqueStageName, List<PipelineStageRequest>> {
	@Override
	public boolean isValid(List<PipelineStageRequest> value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		Set<PipelineStageName> names = new HashSet<>();
		for (PipelineStageRequest stage : value) {
			if (stage != null && stage.stageName() != null && !names.add(stage.stageName())) {
				return false;
			}
		}
		return true;
	}
}
