package com.dorandoran.user.service;

import com.dorandoran.user.client.ChatPushTextClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 푸시 알림 텍스트 생성.
 * - 기본적으로 Chat Service의 LLM 기반 push-text API를 호출해 body 생성
 * - 실패 시 기존 템플릿 기반 title/body로 fallback
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationAgent {

    private final ChatPushTextClient chatPushTextClient;

    /**
     * 푸시 알림 텍스트 생성
     * @return title, body (body는 실제 푸시 본문 및 startMessage에 사용)
     */
    public PushNotificationText generateNotificationText(
            UUID userId, UUID chatbotId, String topic,
            String concept, int intimacyLevel) {

        // topic이 없으면 LLM 호출 의미가 없으므로 바로 fallback
        if (topic == null || topic.isBlank()) {
            log.info("PushNotificationAgent: topic blank, fallback title/body 사용");
            return buildFallbackText(topic, concept, intimacyLevel);
        }

        // 1차: Chat Service LLM 기반 push-text 호출
        try {
            String llmBody = chatPushTextClient.generatePushBody(
                userId,
                chatbotId,
                topic,
                concept,
                intimacyLevel
            );

            if (llmBody != null && !llmBody.isBlank()) {
                String title = buildTitle(topic, concept, intimacyLevel);
                log.info("PushNotificationAgent: LLM 기반 body 생성 성공 - '{}'", llmBody);
                return new PushNotificationText(title, llmBody);
            } else {
                log.warn("PushNotificationAgent: LLM 기반 body 생성 실패(빈 응답), fallback 사용");
            }
        } catch (Exception e) {
            log.error("PushNotificationAgent: LLM 기반 body 생성 중 예외, fallback 사용 - error={}", e.getMessage(), e);
        }

        // 2차: 템플릿 기반 fallback
        return buildFallbackText(topic, concept, intimacyLevel);
    }

    private PushNotificationText buildFallbackText(String topic, String concept, int intimacyLevel) {
        // topic이 null/blank 인 경우를 포함해 안전하게 처리
        String safeTopic = (topic != null && !topic.isBlank()) ? topic : "대화";
        String title = buildTitle(safeTopic, concept, intimacyLevel);
        String body = buildBody(safeTopic, concept, intimacyLevel);
        return new PushNotificationText(title, body);
    }

    private String buildTitle(String topic, String concept, int intimacyLevel) {
        if (topic.length() > 15) {
            return topic.substring(0, 14) + "…";
        }
        return "오늘은 " + topic + " 어때요?";
    }

    private String buildBody(String topic, String concept, int intimacyLevel) {
        String prefix = intimacyLevel >= 2 ? "함께 " : "오늘 ";
        return prefix + topic + "에 대해 이야기해 보세요.";
    }

    public record PushNotificationText(String title, String body) {}
}
