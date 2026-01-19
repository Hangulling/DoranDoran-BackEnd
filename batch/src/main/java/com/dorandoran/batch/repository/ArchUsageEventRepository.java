package com.dorandoran.batch.repository;

import com.dorandoran.batch.entity.ArchUsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArchUsageEventRepository extends JpaRepository<ArchUsageEvent, UUID> {
    boolean existsBySourceUsageEventId(UUID sourceUsageEventId);
    Optional<ArchUsageEvent> findBySourceUsageEventId(UUID sourceUsageEventId);
}


