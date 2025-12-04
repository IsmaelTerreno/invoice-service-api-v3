package com.remotejob.invoiceservice.filter;

import com.remotejob.invoiceservice.util.CorrelationContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to add correlation ID to all HTTP requests for tracking and debugging.
 */
@Slf4j
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Check if correlation ID exists in header, otherwise generate new one
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = CorrelationContext.initCorrelationId();
            } else {
                CorrelationContext.setCorrelationId(correlationId);
            }

            // Add correlation ID to response header
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            log.debug("Request started: {} {} [correlationId={}]", 
                    httpRequest.getMethod(), 
                    httpRequest.getRequestURI(), 
                    correlationId);

            // Continue with the request
            chain.doFilter(request, response);

            log.debug("Request completed: {} {} [correlationId={}, status={}]", 
                    httpRequest.getMethod(), 
                    httpRequest.getRequestURI(), 
                    correlationId,
                    httpResponse.getStatus());

        } finally {
            // Clear MDC after request completes
            CorrelationContext.clear();
        }
    }
}

