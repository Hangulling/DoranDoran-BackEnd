package com.dorandoran.chat.repository;

import com.dorandoran.chat.entity.Chatbot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatbotRepository extends JpaRepository<Chatbot, UUID> {
    // 봇 타입으로 챗봇 찾기
    List<Chatbot> findByBotType(String botType);

    // 활성 챗봇 조회
    List<Chatbot> findByIsActiveTrue();
}
