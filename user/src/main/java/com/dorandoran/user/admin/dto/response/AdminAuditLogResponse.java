package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 감사 로그 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogResponse {
    private Long id;
    private UUID adminUserId;
    private String adminUserEmail;  // 표시용 (adminUserId로 조회)
    private String actionType;
    private String targetType;
    private Long targetId;
    private String summary;
    private Map<String, Object> beforeJson;
    private Map<String, Object> afterJson;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}
