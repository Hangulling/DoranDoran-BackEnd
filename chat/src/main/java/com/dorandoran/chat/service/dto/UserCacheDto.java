package com.dorandoran.chat.service.dto;

import com.dorandoran.chat.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

/**
 * User 엔티티 캐싱용 DTO
 * 순환 참조 방지를 위해 필요한 필드만 포함
 */
@Getter
@AllArgsConstructor
@Builder
public class UserCacheDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private String email;
    private String name;
    private String firstName;
    private String lastName;
    
    public static UserCacheDto from(User user) {
        return UserCacheDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .build();
    }
}

