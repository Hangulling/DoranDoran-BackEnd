package com.dorandoran.store.exception;

/**
 * 권한 없는 접근 시 발생하는 예외
 */
public class UnauthorizedAccessException extends RuntimeException {

  public UnauthorizedAccessException(String message) {
    super(message);
  }

  public UnauthorizedAccessException(String message, Throwable cause) {
    super(message, cause);
  }
}