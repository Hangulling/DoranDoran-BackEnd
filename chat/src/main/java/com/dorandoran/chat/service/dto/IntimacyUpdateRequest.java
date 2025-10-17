package com.dorandoran.chat.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntimacyUpdateRequest {
    @NotNull(message = "친밀도 레벨은 필수입니다")
    @Min(value = 1, message = "친밀도 레벨은 1 이상이어야 합니다")
    @Max(value = 3, message = "친밀도 레벨은 3 이하여야 합니다")
    private Integer intimacyLevel; // 1, 2, 3
}
