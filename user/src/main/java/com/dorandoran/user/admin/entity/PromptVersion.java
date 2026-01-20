package com.dorandoran.user.admin.entity;

import com.dorandoran.user.admin.enums.AgentType;
import com.dorandoran.user.admin.enums.Concept;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 프롬프트 버전 관리 Entity
 */
@Entity
@Table(name = "prompt_versions", schema = "user_schema")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 50)
    private AgentType agentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "concept", nullable = false, length = 20)
    private Concept concept;

    @Column(name = "intimacy_level", nullable = false)
    private Integer intimacyLevel;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "memo", length = 500)
    private String memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_version_id")
    private PromptVersion parentVersion;

    @Column(name = "created_by", nullable = false, columnDefinition = "uuid")
    private java.util.UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
