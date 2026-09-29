package com.apargo.services.audit.api.context;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import com.apargo.services.audit.common.constant.ApiHeaders;
import com.apargo.services.audit.common.constant.ApiPaths;
import com.apargo.services.audit.common.constant.LogKeys;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * First filter of every request: resolves {@code X-Request-Id} (generated if missing or malformed)
 * and echoes it, assigns a trace id (continued from {@code traceparent} only on internal routes),
 * and fills the MDC. The MDC is always cleared afterwards.
 */
public class RequestContextFilter extends OncePerRequestFilter {

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,100}");
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,100}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = safe(request.getHeader(ApiHeaders.REQUEST_ID), SAFE_REQUEST_ID);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        String traceId = ApiPaths.isInternal(request.getRequestURI())
                ? TraceIds.fromTraceparent(request.getHeader(ApiHeaders.TRACEPARENT)).orElseGet(TraceIds::newTraceId)
                : TraceIds.newTraceId();

        response.setHeader(ApiHeaders.REQUEST_ID, requestId);
        try {
            MDC.put(LogKeys.REQUEST_ID, requestId);
            MDC.put(LogKeys.TRACE_ID, traceId);
            putIfSafe(LogKeys.ORG_ID, request.getHeader(ApiHeaders.ORG_ID));
            putIfSafe(LogKeys.PROJECT_ID, request.getHeader(ApiHeaders.PROJECT_ID));
            putIfSafe(LogKeys.USER_ID, request.getHeader(ApiHeaders.USER_ID));
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    /** Header values reach logs only if they look like ids (no log injection). */
    private static void putIfSafe(String key, String value) {
        String safe = safe(value, SAFE_ID);
        if (safe != null) {
            MDC.put(key, safe);
        }
    }

    private static String safe(String value, Pattern pattern) {
        return value != null && pattern.matcher(value).matches() ? value : null;
    }
}
