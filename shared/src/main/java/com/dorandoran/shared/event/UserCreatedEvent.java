package com.dorandoran.shared.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 생성 이벤트
 */
public record UserCreatedEvent(
    UUID userId,
    String email,
    String firstName,
    String lastName,
    String name,
    LocalDateTime createdAt
) {
    
    public static UserCreatedEvent of(UUID userId, String email, String firstName, String lastName, String name) {
        return new UserCreatedEvent(
            userId,
            email,
            firstName,
            lastName,
            name,
            LocalDateTime.now()
        );
    }
}