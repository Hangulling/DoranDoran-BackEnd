package com.dorandoran.user.admin.enums;

import lombok.Getter;

/**
 * 관리 필요 내역 상태
 */
@Getter
public enum ReviewStatus {
    OPEN("OPEN"),
    DONE("DONE");

    private final String value;

    ReviewStatus(String value) {
        this.value = value;
    }

    public static ReviewStatus fromString(String value) {
        if (value == null) {
            return OPEN;
        }
        String normalized = value.toUpperCase();
        for (ReviewStatus status : ReviewStatus.values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        return OPEN;
    }
}
