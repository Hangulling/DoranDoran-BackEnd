package com.dorandoran.batch.repository;

import com.dorandoran.batch.entity.ArchStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArchStoreRepository extends JpaRepository<ArchStore, UUID> {
    boolean existsBySourceStoreId(UUID sourceStoreId);
    Optional<ArchStore> findBySourceStoreId(UUID sourceStoreId);
}


