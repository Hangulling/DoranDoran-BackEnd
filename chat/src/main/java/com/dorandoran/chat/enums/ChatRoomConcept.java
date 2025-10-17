package com.dorandoran.chat.enums;

/**
 * 채팅방 컨셉 Enum
 * 5가지 컨셉과 각각의 기본 친밀도 레벨을 정의
 */
public enum ChatRoomConcept {
    FRIEND("친구", 3),    // 기본 intimacyLevel 3 (반말)
    HONEY("연인", 2),     // 기본 intimacyLevel 2 (존댓말)
    COWORKER("직장 동료", 2),  // 기본 intimacyLevel 2
    SENIOR("학교 선배", 2),  // 기본 intimacyLevel 2
    BOSS("직장 상사", 1);  // 기본 intimacyLevel 1 (격식체)
    
    private final String displayName;
    private final int defaultIntimacyLevel;
    
    ChatRoomConcept(String displayName, int defaultIntimacyLevel) {
        this.displayName = displayName;
        this.defaultIntimacyLevel = defaultIntimacyLevel;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getDefaultIntimacyLevel() {
        return defaultIntimacyLevel;
    }
    
    /**
     * String to Enum 변환 (대소문자 무관, null 처리)
     * 
     * @param value 변환할 문자열
     * @return ChatRoomConcept enum 값
     * @throws IllegalArgumentException 유효하지 않은 컨셉인 경우
     */
    public static ChatRoomConcept fromString(String value) {
        if (value == null || value.isBlank()) {
            return FRIEND;  // 기본값
        }
        try {
            return ChatRoomConcept.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 컨셉: " + value + 
                ". FRIEND, HONEY, COWORKER, SENIOR, BOSS 중 하나를 선택하세요.");
        }
    }
}
