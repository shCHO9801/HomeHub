package io.github.shhome.homehub.common.exception;

import io.github.shhome.homehub.common.dto.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            BusinessException exception
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        log.warn("Business exception occurred. code={}", errorCode.getCode());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiErrorResponse.from(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        FieldError fieldError = exception
                .getBindingResult()
                .getFieldError();

        String message = fieldError != null
                ? fieldError.getDefaultMessage()
                : ErrorCode.INVALID_REQUEST.getMessage();

        String field = fieldError != null
                ? fieldError.getField()
                : null;

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.validation(message, field));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidationException(
            HandlerMethodValidationException exception
    ) {
        String message = ErrorCode.INVALID_REQUEST.getMessage();
        String field = null;

        if (!exception.getParameterValidationResults().isEmpty()) {
            var result = exception
                    .getParameterValidationResults()
                    .getFirst();

            if (!result.getResolvableErrors().isEmpty()) {
                message = result
                        .getResolvableErrors()
                        .getFirst()
                        .getDefaultMessage();
            }

            field = result
                    .getMethodParameter()
                    .getParameterName();
        }

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.validation(message, field));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.validation(
                        "요청 형식이 올바르지 않습니다.",
                        null
                ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.validation(
                        "필수 요청 파라미터가 누락되었습니다.",
                        exception.getParameterName()
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.validation(
                        "요청 파라미터 형식이 올바르지 않습니다.",
                        exception.getName()
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(
            RuntimeException exception
    ) {
        log.error("Unexpected runtime exception occurred.", exception);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.from(errorCode));
    }
}
