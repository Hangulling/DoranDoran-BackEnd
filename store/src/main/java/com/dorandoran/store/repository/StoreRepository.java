package com.dorandoran.store.repository;

import com.dorandoran.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Store Repository
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

  // 사용자별 전체 조회
  List<Store> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

  // 사용자별 전체 조회 (페이징)
  Page<Store> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  // 방별 조회
  List<Store> findByUserIdAndChatroomIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId, UUID chatroomId);

  // 방별 조회 (페이징)
  Page<Store> findByUserIdAndChatroomIdAndIsDeletedFalseOrderByCreatedAtDesc(
      UUID userId, UUID chatroomId, Pageable pageable
  );

  // 중복 저장 확인
  boolean existsByUserIdAndMessageIdAndIsDeletedFalse(UUID userId, UUID messageId);

  // 특정 보관함 조회
  Optional<Store> findByUserIdAndMessageIdAndIsDeletedFalse(UUID userId, UUID messageId);

  // 보관함 개수
  long countByUserIdAndIsDeletedFalse(UUID userId);
}