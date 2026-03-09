package com.dorandoran.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 푸시 알림용 본문(greeting) 텍스트를 LLM(OpenAI)을 사용해 생성하는 서비스.
 * - chat 서비스의 OpenAIClient 인프라를 재사용
 * - 실패 시 간단한 템플릿 기반 fallback 메시지 사용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationTextService {

    private final OpenAIClient openAIClient;

    /**
     * 푸시 알림 body 텍스트 생성
     *
     * @param userId        사용자 ID (선택, 현재는 로그/확장용)
     * @param chatbotId     챗봇 ID (모델 선택 등에 활용 가능, 현재는 로그만)
     * @param topic         대화 주제 (예: "일상", "연애")
     * @param concept       컨셉 (예: friend, honey, senior, coworker)
     * @param intimacyLevel 친밀도 레벨 (1~3, 기본 1)
     * @return 푸시 알림 body로 사용할 한 줄 문구
     */
    public String generatePushBody(UUID userId,
                                   UUID chatbotId,
                                   String topic,
                                   String concept,
                                   int intimacyLevel) {
        String safeConcept = (concept != null && !concept.isBlank()) ? concept.toLowerCase() : "friend";
        String safeTopic = (topic != null && !topic.isBlank()) ? topic : null;
        int level;
        if (intimacyLevel >= 1 && intimacyLevel <= 3) {
            level = intimacyLevel;
        } else {
            log.info("intimacyLevel fallback: using default 1 (PushNotificationTextService.generatePushBody, intimacyLevel={}, valid range 1-3)", intimacyLevel);
            level = 1;
        }

        String systemPrompt = buildSystemPrompt();
        String userMessage = buildUserMessage(safeConcept, safeTopic, level);

        try {
            log.info("PushNotificationTextService: LLM 기반 푸시 문구 생성 시작 - userId={}, chatbotId={}, concept={}, topic={}, intimacyLevel={}",
                userId, chatbotId, safeConcept, safeTopic, level);

            // 채팅방 컨텍스트가 없으므로 chatroomId는 null 로 전달, 기본 모델 사용
            String aiResponse = openAIClient.simpleCompletion(systemPrompt, userMessage, null);
            if (aiResponse == null || aiResponse.isBlank()) {
                log.warn("PushNotificationTextService: LLM 응답이 비어 있음, fallback 사용");
                return buildFallbackBody(safeTopic, safeConcept, level);
            }

            String trimmed = aiResponse.strip();
            int newlineIdx = trimmed.indexOf('\n');
            if (newlineIdx >= 0) {
                trimmed = trimmed.substring(0, newlineIdx).trim();
            }

            // 너무 길면 잘라서 사용 (푸시 UX 보호)
            if (trimmed.length() > 60) {
                trimmed = trimmed.substring(0, 57) + "…";
            }

            log.info("PushNotificationTextService: LLM 기반 푸시 문구 생성 완료 - result='{}'", trimmed);
            return trimmed;
        } catch (Exception e) {
            log.error("PushNotificationTextService: LLM 호출 실패, fallback 사용 - error={}", e.getMessage(), e);
            return buildFallbackBody(safeTopic, safeConcept, level);
        }
    }

    private String buildSystemPrompt() {
        return """
            너는 한국어 학습용 챗봇 "도란도란"의 푸시 알림 문구를 만드는 카피라이터야.
            사용자의 관계 컨셉(concept)과 친밀도(intimacyLevel), 대화 주제(topic)에 맞는
            짧은 한 문장의 초대 메시지를 만들어야 해.

            요구사항:
            - 출력은 반드시 한국어 한 문장이어야 한다.
            - 35자 이내로 자연스럽게 작성한다.
            - 푸시 알림 body로 바로 사용할 수 있도록, 부가 설명이나 따옴표(")는 넣지 않는다.
            - 너무 광고 같지 않게, 편안하게 말을 거는 톤으로 작성한다.
            - 사용자가 대화 주제(topic)를 보고 "아 이걸로 얘기해볼까?" 라고 느끼도록 가볍게 유도한다.
            """;
    }

    private String buildUserMessage(String concept, String topic, int intimacyLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append("컨셉: ").append(concept).append("\n");
        sb.append("친밀도: ").append(intimacyLevel).append("\n");
        if (topic != null) {
            sb.append("주제: ").append(topic).append("\n");
        }
        sb.append("위 정보를 바탕으로 푸시 알림 body로 쓸 짧은 한 문장을 만들어줘.");
        sb.append(" 따옴표 없이 문장만 출력해.");
        return sb.toString();
    }

    private String buildFallbackBody(String topic, String concept, int intimacyLevel) {
        String base;
        if (topic == null || topic.isBlank()) {
            base = "오늘 있었던 이야기, 나랑 천천히 풀어볼래요?";
        } else {
            base = "오늘 " + topic + "에 대해 나랑 이야기해 볼래요?";
        }
        // concept·intimacy 별로 더 세밀하게 나눌 수 있지만, 지금은 공통 fallback만 사용
        return base;
    }
}

