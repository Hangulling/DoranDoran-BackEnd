package com.dorandoran.batch.repository;

import com.dorandoran.batch.entity.ArchChatroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArchChatroomRepository extends JpaRepository<ArchChatroom, UUID> {
    
    Optional<ArchChatroom> findBySourceChatroomId(UUID sourceChatroomId);
    
    @Query("SELECT ac FROM ArchChatroom ac WHERE ac.sourceChatroomId IN :sourceIds")
    List<ArchChatroom> findBySourceChatroomIds(@Param("sourceIds") List<UUID> sourceIds);
    
    boolean existsBySourceChatroomId(UUID sourceChatroomId);
}


