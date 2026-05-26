package com.pipelineforge.security.controller;

import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
	@GetMapping("/overview")
	@PreAuthorize("hasRole('ADMIN')")
	public AdminOverviewResponse overview() {
		return new AdminOverviewResponse("restricted", Instant.now());
	}

	public record AdminOverviewResponse(String status, Instant timestamp) {
	}
}
