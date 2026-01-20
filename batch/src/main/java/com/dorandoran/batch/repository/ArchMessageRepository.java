package com.dorandoran.batch.repository;

import com.dorandoran.batch.entity.ArchMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArchMessageRepository extends JpaRepository<ArchMessage, UUID> {
    
    Optional<ArchMessage> findBySourceMessageId(UUID sourceMessageId);
    
    @Query("SELECT am FROM ArchMessage am WHERE am.archChatroom.id = :archChatroomId ORDER BY am.sequenceNumber ASC")
    List<ArchMessage> findByArchChatroomIdOrderBySequenceNumberAsc(@Param("archChatroomId") UUID archChatroomId);
    
    @Query("SELECT am FROM ArchMessage am WHERE am.sourceMessageId IN :sourceIds")
    List<ArchMessage> findBySourceMessageIds(@Param("sourceIds") List<UUID> sourceIds);
    
    boolean existsBySourceMessageId(UUID sourceMessageId);
}


