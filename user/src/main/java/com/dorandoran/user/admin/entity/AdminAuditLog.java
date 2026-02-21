package com.dorandoran.user.admin.entity;

import com.dorandoran.user.admin.enums.ActionType;
import com.dorandoran.user.admin.enums.TargetType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 관리 감사 로그 Entity
 */
@Entity
@Table(name = "admin_audit_logs", schema = "user_schema")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "admin_user_id", nullable = false, columnDefinition = "uuid")
    private UUID adminUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 50)
    private TargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "summary", length = 500)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_json", columnDefinition = "jsonb")
    private Map<String, Object> beforeJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_json", columnDefinition = "jsonb")
    private Map<String, Object> afterJson;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
