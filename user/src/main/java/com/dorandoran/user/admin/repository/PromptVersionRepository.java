package com.dorandoran.user.admin.repository;

import com.dorandoran.user.admin.entity.PromptVersion;
import com.dorandoran.user.admin.enums.AgentType;
import com.dorandoran.user.admin.enums.Concept;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 프롬프트 버전 Repository
 */
@Repository
public interface PromptVersionRepository extends JpaRepository<PromptVersion, Long> {

    /**
     * 특정 조건의 최신 버전 조회
     */
    Optional<PromptVersion> findTopByAgentTypeAndConceptAndIntimacyLevelOrderByCreatedAtDesc(
            AgentType agentType,
            Concept concept,
            Integer intimacyLevel
    );

    /**
     * 특정 조건의 버전 목록 조회 (페이지네이션)
     */
    @org.springframework.data.jpa.repository.Query("SELECT pv FROM PromptVersion pv " +
           "WHERE pv.agentType = :agentType " +
           "AND pv.concept = :concept " +
           "AND pv.intimacyLevel = :intimacyLevel " +
           "ORDER BY pv.createdAt DESC")
    Page<PromptVersion> findByAgentTypeAndConceptAndIntimacyLevel(
            @org.springframework.data.repository.query.Param("agentType") AgentType agentType,
            @org.springframework.data.repository.query.Param("concept") Concept concept,
            @org.springframework.data.repository.query.Param("intimacyLevel") Integer intimacyLevel,
            Pageable pageable
    );

    /**
     * 버전 번호로 조회
     */
    Optional<PromptVersion> findByAgentTypeAndConceptAndIntimacyLevelAndVersion(
            AgentType agentType,
            Concept concept,
            Integer intimacyLevel,
            String version
    );
}
