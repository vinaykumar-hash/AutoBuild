package com.pipelineforge.deployment.service;

import com.pipelineforge.deployment.dto.PagedResponse;
import com.pipelineforge.deployment.dto.RollbackLogResponse;
import com.pipelineforge.deployment.dto.RollbackRequest;
import com.pipelineforge.deployment.dto.RollbackResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface RollbackService {
	RollbackResponse rollback(RollbackRequest request);

	PagedResponse<RollbackResponse> history(UUID pipelineId, Pageable pageable);

	PagedResponse<RollbackLogResponse> logs(UUID rollbackId, Pageable pageable);
}
