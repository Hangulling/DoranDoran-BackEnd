package com.dorandoran.user.admin.entity;

import com.dorandoran.user.admin.enums.AgentType;
import com.dorandoran.user.admin.enums.Concept;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * PromptActive 복합 키
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PromptActiveId implements Serializable {

    @Column(name = "env", nullable = false, length = 20)
    private String env;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 50)
    private AgentType agentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "concept", nullable = false, length = 20)
    private Concept concept;

    @Column(name = "intimacy_level", nullable = false)
    private Integer intimacyLevel;
}
