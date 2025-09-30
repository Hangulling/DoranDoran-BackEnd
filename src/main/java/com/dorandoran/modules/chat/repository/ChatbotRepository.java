package com.dorandoran.modules.chat.repository;

import com.dorandoran.modules.chat.entity.Chatbot;
import com.dorandoran.shared.database.repository.BaseRepository;
import com.dorandoran.shared.enums.BotType;

import java.util.Optional;
import java.util.UUID;

public interface ChatbotRepository extends BaseRepository<Chatbot, UUID> {

  // 봇 타입으로 조회
  Optional<Chatbot> findByBotType(BotType botType);

  // 봇 타입 존재 여부 확인
  boolean existsByBotType(BotType botType);
}