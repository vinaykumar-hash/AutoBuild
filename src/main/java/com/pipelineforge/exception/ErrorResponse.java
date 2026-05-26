package com.pipelineforge.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		String traceId,
		Map<String, String> validationErrors
) {
}
