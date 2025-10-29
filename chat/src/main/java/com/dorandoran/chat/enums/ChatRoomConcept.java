package com.dorandoran.chat.enums;

/**
 * 채팅방 컨셉 Enum
 * 5가지 컨셉과 각각의 기본 친밀도 레벨을 정의
 */
public enum ChatRoomConcept {
    FRIEND("친구", 3, 1, 3),    // 기본 3, 범위 1-3
    HONEY("연인", 2, 1, 3),     // 기본 2, 범위 1-3
    COWORKER("직장 동료", 2, 1, 3),  // 기본 2, 범위 1-3 (Level 3은 절제된 반말)
    SENIOR("학교 선배", 2, 1, 3),  // 기본 2, 범위 1-3 (Level 3은 절제된 반말)
    BOSS("직장 상사", 1, 1, 2);  // 기본 1, 범위 1-2 (반말 불가)
    
    private final String displayName;
    private final int defaultIntimacyLevel;
    private final int minAllowedLevel;
    private final int maxAllowedLevel;
    
    ChatRoomConcept(String displayName, int defaultIntimacyLevel, int minAllowedLevel, int maxAllowedLevel) {
        this.displayName = displayName;
        this.defaultIntimacyLevel = defaultIntimacyLevel;
        this.minAllowedLevel = minAllowedLevel;
        this.maxAllowedLevel = maxAllowedLevel;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getDefaultIntimacyLevel() {
        return defaultIntimacyLevel;
    }
    
    public int getMinAllowedLevel() {
        return minAllowedLevel;
    }
    
    public int getMaxAllowedLevel() {
        return maxAllowedLevel;
    }
    
    /**
     * 주어진 레벨이 이 컨셉에서 허용되는지 확인
     */
    public boolean isLevelAllowed(int level) {
        return level >= minAllowedLevel && level <= maxAllowedLevel;
    }
    
    /**
     * 유효하지 않은 레벨을 허용 범위로 조정
     */
    public int adjustLevel(int level) {
        if (level < minAllowedLevel) return minAllowedLevel;
        if (level > maxAllowedLevel) return maxAllowedLevel;
        return level;
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
