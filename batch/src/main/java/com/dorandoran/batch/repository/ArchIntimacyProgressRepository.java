package com.dorandoran.batch.repository;

import com.dorandoran.batch.entity.ArchIntimacyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArchIntimacyProgressRepository extends JpaRepository<ArchIntimacyProgress, UUID> {
    boolean existsBySourceIntimacyProgressId(UUID sourceIntimacyProgressId);
    Optional<ArchIntimacyProgress> findBySourceIntimacyProgressId(UUID sourceIntimacyProgressId);
}


