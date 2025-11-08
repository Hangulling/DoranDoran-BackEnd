package com.dorandoran.store.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ChatbotId → BotType 매핑 유틸리티
 *
 * 각 챗봇의 고정 ID를 botType으로 자동 변환
 */
public class BotTypeMapper {

  // chatbotId → botType 매핑 (고정값)
  private static final Map<UUID, String> CHATBOT_TYPE_MAP = new HashMap<>();

  static {
    // 친구 봇 (friend-bot)
    CHATBOT_TYPE_MAP.put(
        UUID.fromString("22222222-2222-2222-2222-222222222221"),
        "friend"
    );

    // 꿀 봇 (honey-bot)
    CHATBOT_TYPE_MAP.put(
        UUID.fromString("22222222-2222-2222-2222-222222222222"),
        "honey"
    );

    // 동료 봇 (coworker-bot)
    CHATBOT_TYPE_MAP.put(
        UUID.fromString("22222222-2222-2222-2222-222222222223"),
        "coworker"
    );

    // 선배 봇 (senior-bot)
    CHATBOT_TYPE_MAP.put(
        UUID.fromString("22222222-2222-2222-2222-222222222224"),
        "senior"
    );
  }

  /**
   * chatbotId로 botType 조회
   *
   * @param chatbotId 챗봇 ID
   * @return botType (friend, honey, coworker, senior)
   * @throws IllegalArgumentException 유효하지 않은 chatbotId
   */
  public static String getBotType(UUID chatbotId) {
    if (chatbotId == null) {
      throw new IllegalArgumentException("chatbotId는 필수입니다");
    }

    String botType = CHATBOT_TYPE_MAP.get(chatbotId);
    if (botType == null) {
      throw new IllegalArgumentException(
          "유효하지 않은 chatbotId입니다: " + chatbotId +
              ". 유효한 챗봇: friend, honey, coworker, senior"
      );
    }
    return botType;
  }

  /**
   * chatbotId가 유효한지 확인
   *
   * @param chatbotId 챗봇 ID
   * @return 유효 여부
   */
  public static boolean isValidChatbotId(UUID chatbotId) {
    return chatbotId != null && CHATBOT_TYPE_MAP.containsKey(chatbotId);
  }

  /**
   * 지원하는 모든 chatbotId 목록 반환
   * (디버깅/로깅용)
   */
  public static Map<UUID, String> getAllMappings() {
    return new HashMap<>(CHATBOT_TYPE_MAP);
  }

  /**
   * botType을 chatroomName으로 변환
   *
   * @param botType 봇 타입 (friend, honey, coworker, senior)
   * @return chatroomName (Friend, Honey, Coworker, Senior)
   */
  public static String getChatroomName(String botType) {
    if (botType == null || botType.isEmpty()) {
      return "Unknown";
    }

    // 첫 글자만 대문자로 변환: friend → Friend
    return botType.substring(0, 1).toUpperCase() + botType.substring(1);
  }
}