package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IntimacyLevel 옵션 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntimacyLevelOption {
    private Integer value;
    private String label;
}
