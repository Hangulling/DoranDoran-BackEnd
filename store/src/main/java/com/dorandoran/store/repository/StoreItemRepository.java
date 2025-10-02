package com.dorandoran.store.repository;

import com.dorandoran.store.entity.StoreItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoreItemRepository extends JpaRepository<StoreItem, UUID> {
    // 사용자 ID로 저장소 아이템 목록 찾기
    List<StoreItem> findByUserId(UUID userId);
}
