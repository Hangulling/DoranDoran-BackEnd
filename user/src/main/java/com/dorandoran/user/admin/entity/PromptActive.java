package com.dorandoran.user.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Active 프롬프트 관리 Entity
 */
@Entity
@Table(name = "prompt_actives", schema = "user_schema")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptActive {

    @EmbeddedId
    private PromptActiveId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_version_id", nullable = false)
    private PromptVersion promptVersion;

    @Column(name = "activated_by", nullable = false, columnDefinition = "uuid")
    private UUID activatedBy;

    @CreationTimestamp
    @Column(name = "activated_at", nullable = false, updatable = false)
    private LocalDateTime activatedAt;
}
