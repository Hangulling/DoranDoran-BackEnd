package com.dorandoran.chat.service.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomUpdateRequest {
    @Size(min = 1, max = 100, message = "채팅방 이름은 1-100자 사이여야 합니다")
    private String name;

    @Size(max = 1000, message = "설명은 0-1000자 사이여야 합니다")
    private String description;

    private Boolean archived;
}


