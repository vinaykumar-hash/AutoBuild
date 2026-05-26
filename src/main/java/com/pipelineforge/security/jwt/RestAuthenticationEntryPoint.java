package com.pipelineforge.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipelineforge.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
	private final ObjectMapper objectMapper;

	public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException
	) throws IOException {
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				HttpStatus.UNAUTHORIZED.value(),
				HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				"Authentication required",
				request.getRequestURI(),
				String.valueOf(request.getAttribute("traceId")),
				Map.of()
		);
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType("application/json");
		objectMapper.writeValue(response.getOutputStream(), error);
	}
}
