package com.apargo.services.audit.common.response;

import java.util.List;

import com.apargo.services.audit.common.error.ErrorCode;

/**
 * The platform response wrapper (API standard §4). {@code status} always equals the HTTP
 * status, and {@code success} is true only for 2xx.
 */
public record ApiResponse<T>(
        boolean success,
        int status,
        String code,
        String message,
        T data,
        List<FieldError> errors,
        ApiMeta meta) {

    public static final String SUCCESS_CODE = "SUCCESS";
    private static final int OK = 200;

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, OK, SUCCESS_CODE, message, data, List.of(), ApiMeta.current(null));
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message, List<FieldError> errors, String path) {
        return new ApiResponse<>(false, errorCode.httpStatus(), errorCode.name(),
                message != null ? message : errorCode.defaultMessage(),
                null, errors == null ? List.of() : List.copyOf(errors), ApiMeta.current(path));
    }
}
