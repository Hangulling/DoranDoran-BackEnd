package com.dorandoran.batch.repository;

import com.dorandoran.batch.entity.ArchIngestionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArchIngestionStateRepository extends JpaRepository<ArchIngestionState, Long> {
    
    Optional<ArchIngestionState> findByJobName(String jobName);
}


