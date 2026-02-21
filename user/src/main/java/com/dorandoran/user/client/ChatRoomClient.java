package com.dorandoran.user.client;

import com.dorandoran.shared.security.HmacVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomClient {

    private final RestTemplate restTemplate;

    @Value("${chat.service.url:http://localhost:8083}")
    private String chatServiceUrl;

    @Value("${gateway.jwt.hmac-secret:}")
    private String hmacSecret;

    public List<ChatRoomSummary> listChatRooms(UUID userId) {
        String url = String.format("%s/api/chat/chatrooms?userId=%s&page=0&size=200", chatServiceUrl, userId);
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(buildHmacHeaders(userId));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            List<ChatRoomSummary> rooms = new ArrayList<>();
            if (response.getBody() != null && response.getBody().get("content") instanceof List<?> content) {
                for (Object item : content) {
                    if (item instanceof Map<?, ?> map) {
                        Object id = map.get("id");
                        Object lastMessageId = map.get("lastMessageId");
                        if (id != null) {
                            rooms.add(new ChatRoomSummary(
                                UUID.fromString(id.toString()),
                                lastMessageId != null && !lastMessageId.toString().isBlank()
                                    ? UUID.fromString(lastMessageId.toString())
                                    : null
                            ));
                        }
                    }
                }
            }
            return rooms;
        } catch (Exception e) {
            log.warn("채팅방 목록 조회 실패: userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }

    public String getMessageContent(UUID userId, UUID messageId) {
        String url = String.format("%s/api/chat/messages/%s?userId=%s", chatServiceUrl, messageId, userId);
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(buildHmacHeaders(userId));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getBody() != null) {
                Object content = response.getBody().get("content");
                return content != null ? content.toString() : null;
            }
            return null;
        } catch (Exception e) {
            log.warn("메시지 조회 실패: userId={}, messageId={}, error={}", userId, messageId, e.getMessage());
            return null;
        }
    }

    public record ChatRoomSummary(UUID id, UUID lastMessageId) {}

    private Map<String, String> buildHmacHeaders(UUID userId) {
        long ts = System.currentTimeMillis();
        String message = userId + "|" + ts;
        String sign = HmacVerifier.hmacSha256Hex(hmacSecret, message);
        return Map.of(
            "X-User-Id", userId.toString(),
            "X-Auth-Ts", Long.toString(ts),
            "X-Auth-Sign", sign
        );
    }
}
