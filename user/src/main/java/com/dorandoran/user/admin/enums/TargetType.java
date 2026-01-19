package com.dorandoran.user.admin.enums;

import lombok.Getter;

/**
 * 관리 감사 로그 대상 타입
 */
@Getter
public enum TargetType {
    PROMPT_VERSION("PROMPT_VERSION"),
    REVIEW_TICKET("REVIEW_TICKET");

    private final String value;

    TargetType(String value) {
        this.value = value;
    }

    public static TargetType fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toUpperCase();
        for (TargetType targetType : TargetType.values()) {
            if (targetType.name().equals(normalized)) {
                return targetType;
            }
        }
        return null;
    }
}
