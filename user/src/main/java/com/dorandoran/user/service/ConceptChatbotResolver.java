package com.dorandoran.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * concept(honey/friend/senior/coworker) 별 기본 챗봇 ID를 resolve 하는 헬퍼.
 * 기본값은 프런트엔드의 CONCEPT_TO_CHATBOT_ID_MAP 과 동일한 UUID를 사용한다.
 * 설정 파일이나 환경 변수로 override 가능하다.
 */
@Component
@Slf4j
public class ConceptChatbotResolver {

    private final Map<String, UUID> conceptToChatbotId;

    public ConceptChatbotResolver(
        @Value("${notification.chatbots.friend-id:22222222-2222-2222-2222-222222222221}") String friendId,
        @Value("${notification.chatbots.honey-id:22222222-2222-2222-2222-222222222222}") String honeyId,
        @Value("${notification.chatbots.coworker-id:22222222-2222-2222-2222-222222222223}") String coworkerId,
        @Value("${notification.chatbots.senior-id:22222222-2222-2222-2222-222222222224}") String seniorId
    ) {
        this.conceptToChatbotId = Map.of(
            "friend", UUID.fromString(friendId),
            "honey", UUID.fromString(honeyId),
            "coworker", UUID.fromString(coworkerId),
            "senior", UUID.fromString(seniorId)
        );
    }

    /**
     * concept 문자열(honey/friend/senior/coworker)을 받아서 대응되는 챗봇 ID 를 반환한다.
     * 알 수 없는 concept 인 경우 null 을 반환하고 로그를 남긴다.
     */
    public UUID resolve(String concept) {
        if (concept == null || concept.isBlank()) {
            log.warn("ConceptChatbotResolver: concept blank, cannot resolve chatbotId");
            return null;
        }
        String key = concept.toLowerCase(Locale.ROOT);
        UUID id = conceptToChatbotId.get(key);
        if (id == null) {
            log.warn("ConceptChatbotResolver: unsupported concept={}, cannot resolve chatbotId", concept);
        }
        return id;
    }
}

