package com.dorandoran.user.admin.repository;

import com.dorandoran.user.admin.entity.PromptActive;
import com.dorandoran.user.admin.entity.PromptActiveId;
import com.dorandoran.user.admin.enums.AgentType;
import com.dorandoran.user.admin.enums.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Active 프롬프트 Repository
 */
@Repository
public interface PromptActiveRepository extends JpaRepository<PromptActive, PromptActiveId> {

    /**
     * 특정 조건의 Active 프롬프트 조회
     */
    @Query("SELECT pa FROM PromptActive pa " +
           "WHERE pa.id.env = :env " +
           "AND pa.id.agentType = :agentType " +
           "AND pa.id.concept = :concept " +
           "AND pa.id.intimacyLevel = :intimacyLevel")
    Optional<PromptActive> findByEnvAndAgentTypeAndConceptAndIntimacyLevel(
            @Param("env") String env,
            @Param("agentType") AgentType agentType,
            @Param("concept") Concept concept,
            @Param("intimacyLevel") Integer intimacyLevel
    );

    /**
     * 특정 환경의 모든 Active 프롬프트 조회
     */
    @Query("SELECT pa FROM PromptActive pa WHERE pa.id.env = :env")
    java.util.List<PromptActive> findAllByEnv(@Param("env") String env);
}
