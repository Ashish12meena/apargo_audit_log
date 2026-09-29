package com.apargo.services.audit.api.context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import com.apargo.services.audit.api.error.ErrorResponseWriter;
import com.apargo.services.audit.common.constant.ApiHeaders;
import com.apargo.services.audit.common.constant.ApiPaths;
import com.apargo.services.audit.common.constant.LogKeys;
import com.apargo.services.audit.common.error.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Guards {@code /internal/**}: requires {@code X-Internal-Api-Key} (constant-time comparison) and
 * {@code X-Internal-Caller}, from the allow-list unless any caller is allowed. With a blank key every
 * internal call is refused.
 */
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalApiKeyFilter.class);
    private static final int MAX_CALLER_LENGTH = 100;

    private final byte[] expectedKey;
    private final Set<String> allowedCallers;
    private final boolean anyCaller;
    private final ErrorResponseWriter errorWriter;

    public InternalApiKeyFilter(String apiKey, Set<String> allowedCallers, boolean anyCaller,
                                ErrorResponseWriter errorWriter) {
        this.expectedKey = apiKey == null || apiKey.isBlank() ? null : apiKey.getBytes(StandardCharsets.UTF_8);
        this.allowedCallers = Set.copyOf(allowedCallers);
        this.anyCaller = anyCaller;
        this.errorWriter = errorWriter;
        if (expectedKey == null) {
            log.warn("internal_api_disabled reason=no_api_key_configured");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !ApiPaths.isInternal(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String caller = request.getHeader(ApiHeaders.INTERNAL_CALLER);
        String key = request.getHeader(ApiHeaders.INTERNAL_API_KEY);

        if (expectedKey == null || key == null) {
            reject(request, response, ErrorCode.UNAUTHENTICATED, "Internal API key is required", caller);
            return;
        }
        if (!MessageDigest.isEqual(expectedKey, key.getBytes(StandardCharsets.UTF_8))) {
            reject(request, response, ErrorCode.UNAUTHENTICATED, "Internal API key is not valid", caller);
            return;
        }
        if (caller == null || caller.isBlank() || caller.length() > MAX_CALLER_LENGTH
                || (!anyCaller && !allowedCallers.contains(caller))) {
            reject(request, response, ErrorCode.FORBIDDEN, "Caller is not allowed", caller);
            return;
        }
        MDC.put(LogKeys.CALLER_SERVICE, caller);
        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, ErrorCode code, String message,
                        String caller) throws IOException {
        log.warn("internal_request_rejected path={} code={} caller={}", request.getRequestURI(), code,
                caller == null ? "-" : caller.substring(0, Math.min(caller.length(), MAX_CALLER_LENGTH)));
        errorWriter.write(request, response, code, message);
    }
}
