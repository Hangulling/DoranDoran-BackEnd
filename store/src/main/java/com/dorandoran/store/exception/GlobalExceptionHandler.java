package com.dorandoran.store.exception;

import com.dorandoran.store.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 전역 예외 핸들러
 * 모든 예외를 통일된 형식으로 응답
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * 보관함 항목을 찾을 수 없음 (404)
   */
  @ExceptionHandler(BookmarkNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleBookmarkNotFound(
      BookmarkNotFoundException ex,
      HttpServletRequest request) {

    log.error("BookmarkNotFoundException: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .status(HttpStatus.NOT_FOUND.value())
        .code("BOOKMARK_NOT_FOUND")
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  /**
   * 중복 저장 시도 (409)
   */
  @ExceptionHandler(DuplicateBookmarkException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateBookmark(
      DuplicateBookmarkException ex,
      HttpServletRequest request) {

    log.error("DuplicateBookmarkException: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .status(HttpStatus.CONFLICT.value())
        .code("DUPLICATE_BOOKMARK")
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  /**
   * 권한 없는 접근 (403)
   */
  @ExceptionHandler(UnauthorizedAccessException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
      UnauthorizedAccessException ex,
      HttpServletRequest request) {

    log.error("UnauthorizedAccessException: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .status(HttpStatus.FORBIDDEN.value())
        .code("UNAUTHORIZED_ACCESS")
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  /**
   * 잘못된 요청 (400)
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex,
      HttpServletRequest request) {

    log.error("IllegalArgumentException: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .code("INVALID_REQUEST")
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * 잘못된 상태 (400)
   */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalState(
      IllegalStateException ex,
      HttpServletRequest request) {

    log.error("IllegalStateException: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .code("INVALID_STATE")
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * 유효성 검증 실패 (400)
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationError(
      MethodArgumentNotValidException ex,
      HttpServletRequest request) {

    log.error("MethodArgumentNotValidException: {}", ex.getMessage());

    BindingResult bindingResult = ex.getBindingResult();

    List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
        .map(fieldError -> ErrorResponse.FieldError.builder()
            .field(fieldError.getField())
            .message(fieldError.getDefaultMessage())
            .rejectedValue(fieldError.getRejectedValue())
            .build())
        .collect(Collectors.toList());

    ErrorResponse error = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .code("VALIDATION_ERROR")
        .message("유효성 검증 실패")
        .path(request.getRequestURI())
        .errors(fieldErrors)
        .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * 예상치 못한 에러 (500)
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex,
      HttpServletRequest request) {

    log.error("Unexpected Exception: ", ex);

    ErrorResponse error = ErrorResponse.builder()
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .code("INTERNAL_SERVER_ERROR")
        .message("서버 내부 오류가 발생했습니다")
        .detail(ex.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}