package com.pipelineforge.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTracingFilter implements Filter {
	private static final String TRACE_ID_KEY = "traceId";
	private static final String TRACE_HEADER = "X-Trace-Id";
	private static final String CORRELATION_HEADER = "X-Correlation-Id";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
			String traceId = httpRequest.getHeader(TRACE_HEADER);
			if (traceId == null || traceId.isBlank()) {
				traceId = httpRequest.getHeader(CORRELATION_HEADER);
			}
			if (traceId == null || traceId.isBlank()) {
				traceId = UUID.randomUUID().toString().replace("-", "");
			}

			MDC.put(TRACE_ID_KEY, traceId);
			httpRequest.setAttribute(TRACE_ID_KEY, traceId);
			httpResponse.setHeader(TRACE_HEADER, traceId);

			try {
				chain.doFilter(request, response);
			} finally {
				MDC.remove(TRACE_ID_KEY);
			}
		} else {
			chain.doFilter(request, response);
		}
	}
}
