package com.pipelineforge.pipeline.validation;

import com.pipelineforge.pipeline.dto.PipelineStageRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UniqueExecutionOrderValidator implements ConstraintValidator<UniqueExecutionOrder, List<PipelineStageRequest>> {
	@Override
	public boolean isValid(List<PipelineStageRequest> value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		Set<Integer> orders = new HashSet<>();
		for (PipelineStageRequest stage : value) {
			if (stage != null && !orders.add(stage.executionOrder())) {
				return false;
			}
		}
		return true;
	}
}
