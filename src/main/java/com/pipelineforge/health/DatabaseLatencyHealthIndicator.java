package com.pipelineforge.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseLatencyHealthIndicator implements HealthIndicator {
	private final JdbcTemplate jdbcTemplate;

	public DatabaseLatencyHealthIndicator(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Health health() {
		long start = System.currentTimeMillis();
		try {
			jdbcTemplate.execute("SELECT 1");
			long latency = System.currentTimeMillis() - start;

			if (latency > 500) {
				return Health.down()
						.withDetail("message", "Database responds slow")
						.withDetail("latencyMs", latency)
						.build();
			}

			return Health.up()
					.withDetail("message", "Database responds fast")
					.withDetail("latencyMs", latency)
					.build();
		} catch (Exception e) {
			return Health.down(e)
					.withDetail("message", "Database connection error")
					.build();
		}
	}
}
