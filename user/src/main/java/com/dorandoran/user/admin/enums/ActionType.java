package com.dorandoran.user.admin.enums;

import lombok.Getter;

/**
 * 관리 감사 로그 작업 타입
 */
@Getter
public enum ActionType {
    PROMPT_CREATE("PROMPT_CREATE"),
    PROMPT_ACTIVATE("PROMPT_ACTIVATE"),
    PROMPT_ROLLBACK("PROMPT_ROLLBACK"),
    REVIEW_EXPORT("REVIEW_EXPORT"),
    REVIEW_COMPLETE("REVIEW_COMPLETE"),
    REVIEW_DELETE("REVIEW_DELETE");

    private final String value;

    ActionType(String value) {
        this.value = value;
    }

    public static ActionType fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toUpperCase();
        for (ActionType actionType : ActionType.values()) {
            if (actionType.name().equals(normalized)) {
                return actionType;
            }
        }
        return null;
    }
}
