package com.dorandoran.shared.event;

import com.dorandoran.shared.dto.UserDto;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 상태 변경 이벤트
 */
public record UserStatusChangedEvent(
    UUID userId,
    String email,
    UserDto.UserStatus oldStatus,
    UserDto.UserStatus newStatus,
    LocalDateTime changedAt
) {
    
    public static UserStatusChangedEvent of(UUID userId, String email, 
                                          UserDto.UserStatus oldStatus, UserDto.UserStatus newStatus) {
        return new UserStatusChangedEvent(
            userId,
            email,
            oldStatus,
            newStatus,
            LocalDateTime.now()
        );
    }
}
