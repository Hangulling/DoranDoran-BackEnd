package com.dorandoran.user.exception;

import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DoranDoranException.class)
    public ResponseEntity<ApiResponse<Void>> handleDoranDoranException(DoranDoranException ex) {
        ErrorCode code = ex.getErrorCode();

        // 현재 서비스에서 사용 중인 에러코드 기준으로 최소 매핑
        HttpStatus status = switch (code) {
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(ex.getMessage(), code.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(err -> formatFieldError(err))
            .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = ErrorCode.VALIDATION_ERROR.getMessage();
        }

        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(message, ErrorCode.VALIDATION_ERROR.getCode()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("리소스 없음: {}", ex.getResourcePath());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("요청한 경로를 찾을 수 없습니다.", ErrorCode.INVALID_REQUEST.getCode()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("요청 본문 파싱 실패: {}", ex.getMessage());
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error("요청 본문 형식이 올바르지 않습니다.", ErrorCode.INVALID_REQUEST.getCode()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(err -> formatFieldError(err))
            .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = ErrorCode.INVALID_REQUEST.getMessage();
        }

        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(message, ErrorCode.INVALID_REQUEST.getCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("예상치 못한 오류", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }

    private String formatFieldError(FieldError err) {
        String field = err.getField();
        String msg = err.getDefaultMessage();
        if (msg == null || msg.isBlank()) {
            msg = "유효하지 않은 값입니다.";
        }
        return field + ": " + msg;
    }
}

