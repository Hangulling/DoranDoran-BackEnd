package com.dorandoran.batch.repository;

import com.dorandoran.batch.entity.ArchAgentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArchAgentResultRepository extends JpaRepository<ArchAgentResult, UUID> {
    
    @Query("SELECT aar FROM ArchAgentResult aar WHERE aar.archMessage.id = :archMessageId AND aar.agentType = :agentType")
    Optional<ArchAgentResult> findByArchMessageIdAndAgentType(@Param("archMessageId") UUID archMessageId, @Param("agentType") String agentType);
    
    @Query("SELECT aar FROM ArchAgentResult aar WHERE aar.archMessage.id = :archMessageId")
    List<ArchAgentResult> findByArchMessageId(@Param("archMessageId") UUID archMessageId);
}


