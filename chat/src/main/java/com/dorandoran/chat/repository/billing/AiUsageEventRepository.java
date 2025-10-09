package com.dorandoran.chat.repository.billing;

import com.dorandoran.chat.entity.billing.AiUsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiUsageEventRepository extends JpaRepository<AiUsageEvent, UUID> {
}