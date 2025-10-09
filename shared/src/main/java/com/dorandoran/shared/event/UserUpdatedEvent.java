package com.dorandoran.shared.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 업데이트 이벤트
 */
public record UserUpdatedEvent(
    UUID userId,
    String email,
    String firstName,
    String lastName,
    String name,
    String picture,
    String info,
    LocalDateTime updatedAt
) {
    
    public static UserUpdatedEvent of(UUID userId, String email, String firstName, String lastName, 
                                    String name, String picture, String info) {
        return new UserUpdatedEvent(
            userId,
            email,
            firstName,
            lastName,
            name,
            picture,
            info,
            LocalDateTime.now()
        );
    }
}