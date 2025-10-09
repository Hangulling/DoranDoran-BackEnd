package com.dorandoran.common.exception;

/**
 * DoranDoran 애플리케이션 커스텀 예외
 */
public class DoranDoranException extends RuntimeException {
    private final ErrorCode errorCode;
    
    public DoranDoranException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public DoranDoranException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public DoranDoranException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
