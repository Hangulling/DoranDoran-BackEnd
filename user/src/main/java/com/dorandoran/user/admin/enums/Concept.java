package com.dorandoran.user.admin.enums;

import lombok.Getter;

/**
 * 프롬프트 컨셉
 */
@Getter
public enum Concept {
    FRIEND("friend"),
    COWORKER("coworker"),
    BOSS("boss"),
    SENIOR("senior"),
    HONEY("honey");

    private final String value;

    Concept(String value) {
        this.value = value;
    }

    public static Concept fromString(String value) {
        if (value == null) {
            return FRIEND;
        }
        String normalized = value.toUpperCase();
        for (Concept concept : Concept.values()) {
            if (concept.name().equals(normalized)) {
                return concept;
            }
        }
        return FRIEND;
    }
}
