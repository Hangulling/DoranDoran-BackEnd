package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AgentType 옵션 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTypeOption {
    private String value;
    private String label;
}
