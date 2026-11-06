package io.github.shhome.homehub.common.dto;

import io.github.shhome.homehub.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
        int status,
        String message,
        String code,
        String field
) {

    public static ApiErrorResponse from(ErrorCode errorCode) {
        return new ApiErrorResponse(
                errorCode.getStatus().value(),
                errorCode.getMessage(),
                errorCode.getCode(),
                null
        );
    }

    public static ApiErrorResponse validation(
            String message,
            String field
    ) {
        return new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                null,
                field
        );
    }
}
