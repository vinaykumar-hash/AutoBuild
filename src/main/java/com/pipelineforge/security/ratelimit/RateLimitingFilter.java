package com.pipelineforge.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipelineforge.exception.ErrorResponse;
import com.pipelineforge.security.config.RateLimitProperties;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RateLimitingFilter implements Filter {
	private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

	private final StringRedisTemplate redisTemplate;
	private final RateLimitProperties rateLimitProperties;
	private final ObjectMapper objectMapper;

	public RateLimitingFilter(
			StringRedisTemplate redisTemplate,
			RateLimitProperties rateLimitProperties,
			ObjectMapper objectMapper
	) {
		this.redisTemplate = redisTemplate;
		this.rateLimitProperties = rateLimitProperties;
		this.objectMapper = objectMapper;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (!rateLimitProperties.enabled() || !(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
			chain.doFilter(request, response);
			return;
		}

		String path = httpRequest.getRequestURI();

		// Bypass rate limiting for public endpoints, health checks, Swagger docs, and GitHub webhooks
		if (!path.startsWith("/api/") || path.equals("/api/v1/health") || path.startsWith("/api/v1/repositories/webhooks") || path.startsWith("/actuator")) {
			chain.doFilter(request, response);
			return;
		}

		String clientKey = resolveClientKey(httpRequest);
		String redisKey = "ratelimit:" + clientKey;

		try {
			Long currentRequestCount = redisTemplate.opsForValue().increment(redisKey);
			if (currentRequestCount != null) {
				if (currentRequestCount == 1) {
					redisTemplate.expire(redisKey, Duration.ofSeconds(rateLimitProperties.durationSeconds()));
				}

				if (currentRequestCount > rateLimitProperties.limit()) {
					logger.warn("Rate limit exceeded for client key: {}. Current count: {}, limit: {}",
							clientKey, currentRequestCount, rateLimitProperties.limit());

					sendErrorResponse(httpRequest, httpResponse);
					return;
				}
			}
		} catch (Exception e) {
			// Fail open on Redis connectivity issues in production to avoid system outage
			logger.error("Error occurred while verifying rate limit, failing open", e);
		}

		chain.doFilter(request, response);
	}

	private String resolveClientKey(HttpServletRequest request) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
			return "user:" + auth.getName();
		}

		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		if (ip != null && ip.contains(",")) {
			ip = ip.split(",")[0].trim();
		}
		return "ip:" + ip;
	}

	private void sendErrorResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String traceId = (String) request.getAttribute("traceId");
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				HttpStatus.TOO_MANY_REQUESTS.value(),
				HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
				"Rate limit exceeded. Please try again later.",
				request.getRequestURI(),
				traceId,
				Map.of()
		);

		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType("application/json");
		objectMapper.writeValue(response.getOutputStream(), error);
	}
}
