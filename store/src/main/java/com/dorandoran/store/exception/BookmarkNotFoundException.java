package com.dorandoran.store.exception;

/**
 * 보관함 항목을 찾을 수 없을 때 발생하는 예외
 */
public class BookmarkNotFoundException extends RuntimeException {

  public BookmarkNotFoundException(String message) {
    super(message);
  }

  public BookmarkNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}