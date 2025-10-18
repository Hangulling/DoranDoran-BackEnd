package com.dorandoran.store.exception;

/**
 * 중복 저장 시도 시 발생하는 예외
 */
public class DuplicateBookmarkException extends RuntimeException {

  public DuplicateBookmarkException(String message) {
    super(message);
  }

  public DuplicateBookmarkException(String message, Throwable cause) {
    super(message, cause);
  }
}