package com.apargo.services.audit.api.context;

import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.apargo.platform.contract.audit.AuditEventDto;
import com.apargo.services.audit.common.constant.ApiHeaders;
import com.apargo.services.audit.common.constant.ApiPaths;
import com.apargo.services.audit.common.constant.LogKeys;
import com.apargo.services.audit.common.error.ApiException;
import com.apargo.services.audit.common.error.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Builds the {@link RequestContext} controller parameter from headers. Public routes need a
 * positive {@code X-Org-Id}; platform-level data ({@code orgId 0}) is readable only internally.
 */
public class RequestContextResolver implements HandlerMethodArgumentResolver {

    private static final int MAX_USER_ID_LENGTH = 100;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return RequestContext.class.equals(parameter.getParameterType());
    }

    @Override
    public RequestContext resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        boolean internal = request != null && ApiPaths.isInternal(request.getRequestURI());

        Long orgId = parseId(webRequest.getHeader(ApiHeaders.ORG_ID), ApiHeaders.ORG_ID);
        if (!internal) {
            if (orgId == null) {
                throw new ApiException(ErrorCode.BAD_REQUEST, ApiHeaders.ORG_ID + " header is required");
            }
            if (orgId == AuditEventDto.PLATFORM_ORG_ID) {
                throw new ApiException(ErrorCode.FORBIDDEN, "Platform-level logs are not available on this route");
            }
        }
        Long projectId = parseId(webRequest.getHeader(ApiHeaders.PROJECT_ID), ApiHeaders.PROJECT_ID);
        if (projectId != null && projectId == 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST, ApiHeaders.PROJECT_ID + " must be a positive number");
        }
        String userId = webRequest.getHeader(ApiHeaders.USER_ID);
        if (userId != null && (userId.isBlank() || userId.length() > MAX_USER_ID_LENGTH)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, ApiHeaders.USER_ID + " is not valid");
        }
        return new RequestContext(orgId, projectId, userId, internal ? MDC.get(LogKeys.CALLER_SERVICE) : null,
                internal);
    }

    /** Absent → null; present → a non-negative number, otherwise 400. */
    private static Long parseId(String value, String header) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(value.trim());
            if (id >= 0) {
                return id;
            }
        } catch (NumberFormatException e) {
            // reported below
        }
        throw new ApiException(ErrorCode.BAD_REQUEST, header + " must be a non-negative number");
    }
}
