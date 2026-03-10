package com.dorandoran.user.admin.service;

import com.dorandoran.user.admin.entity.AdminAuditLog;
import com.dorandoran.user.admin.enums.ActionType;
import com.dorandoran.user.admin.enums.TargetType;
import com.dorandoran.user.admin.repository.AdminAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 관리 감사 로그 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuditLogService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    /**
     * 감사 로그 기록
     */
    @Transactional
    public void logAction(
            ActionType actionType,
            TargetType targetType,
            Long targetId,
            String summary,
            Map<String, Object> beforeJson,
            Map<String, Object> afterJson,
            UUID adminUserId,
            HttpServletRequest request
    ) {
        AdminAuditLog auditLog = AdminAuditLog.builder()
            .adminUserId(adminUserId)
            .actionType(actionType)
            .targetType(targetType)
            .targetId(targetId)
            .summary(summary)
            .beforeJson(beforeJson)
            .afterJson(afterJson)
            .ip(getClientIp(request))
            .userAgent(request != null ? request.getHeader("User-Agent") : null)
            .build();

        adminAuditLogRepository.save(auditLog);
        log.info("감사 로그 기록: actionType={}, targetType={}, targetId={}, adminUserId={}",
            actionType, targetType, targetId, adminUserId);
    }

    /**
     * 감사 로그 조회
     * 필터가 모두 null이면 단순 목록 조회(JPQL null 바인딩 이슈 회피)
     */
    @Transactional(readOnly = true)
    public Page<AdminAuditLog> getAuditLogs(UUID adminUserId, ActionType actionType, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        if (adminUserId == null && actionType == null && from == null && to == null) {
            return adminAuditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return adminAuditLogRepository.findByConditions(adminUserId, actionType, from, to, pageable);
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // X-Forwarded-For는 여러 IP가 있을 수 있으므로 첫 번째 IP만 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}
