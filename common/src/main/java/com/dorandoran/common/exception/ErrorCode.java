package com.dorandoran.common.exception;

/**
 * 에러 코드 열거형
 */
public enum ErrorCode {
    // 사용자 관련
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다"),
    EMAIL_ALREADY_EXISTS("U002", "이미 존재하는 이메일입니다"),
    INVALID_PASSWORD("U003", "비밀번호가 올바르지 않습니다"),
    USER_ALREADY_INACTIVE("U004", "이미 비활성화된 사용자입니다"),
    USER_ALREADY_SUSPENDED("U005", "이미 정지된 사용자입니다"),
    
    // 인증 관련 (확장 가능)
    AUTH_TOKEN_EXPIRED("A001", "인증 토큰이 만료되었습니다"),
    AUTH_TOKEN_INVALID("A002", "유효하지 않은 인증 토큰입니다"),
    AUTH_ACCESS_DENIED("A003", "접근 권한이 없습니다"),
    
    // 채팅 관련
    CHAT_ROOM_NOT_FOUND("C001", "채팅방을 찾을 수 없습니다"),
    MESSAGE_NOT_FOUND("C002", "메시지를 찾을 수 없습니다"),
    
    // 스토어 관련
    STORE_ITEM_NOT_FOUND("S001", "저장소 항목을 찾을 수 없습니다"),
    
    // 공통
    INTERNAL_SERVER_ERROR("E001", "내부 서버 오류가 발생했습니다"),
    INVALID_REQUEST("E002", "잘못된 요청입니다"),
    VALIDATION_ERROR("E003", "입력값 검증에 실패했습니다");
    
    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
