package com.dorandoran.user.repository;

import com.dorandoran.user.entity.SocialPostBackup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SocialPostBackupRepository extends JpaRepository<SocialPostBackup, Long> {

    Page<SocialPostBackup> findBySourceDomainOrderByFetchedAtDesc(String sourceDomain, Pageable pageable);

    Page<SocialPostBackup> findAllByOrderByFetchedAtDesc(Pageable pageable);

    Optional<SocialPostBackup> findBySourceDomainAndExternalId(String sourceDomain, String externalId);

    @Query("SELECT s FROM SocialPostBackup s ORDER BY s.fetchedAt DESC")
    Page<SocialPostBackup> findAllPaged(Pageable pageable);

    @Query("SELECT s FROM SocialPostBackup s WHERE s.sourceDomain = :sourceDomain ORDER BY s.fetchedAt DESC")
    Page<SocialPostBackup> findBySourceDomainPaged(@Param("sourceDomain") String sourceDomain, Pageable pageable);
}
