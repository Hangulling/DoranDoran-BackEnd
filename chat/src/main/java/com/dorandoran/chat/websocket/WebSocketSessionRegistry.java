package com.dorandoran.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket 세션을 chatroomId별로 관리하고, AI 이벤트를 WebSocket 클라이언트에 전송합니다.
 * iOS에서 SSE가 동작하지 않으므로 WebSocket으로 실시간 이벤트를 전달합니다.
 */
@Component
@Slf4j
public class WebSocketSessionRegistry {

	private final Map<UUID, List<WebSocketSession>> sessionsByChatroom = new ConcurrentHashMap<>();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public void register(UUID chatroomId, WebSocketSession session) {
		sessionsByChatroom.computeIfAbsent(chatroomId, k -> new CopyOnWriteArrayList<>()).add(session);
		log.debug("[WS] 세션 등록: chatroomId={}, sessionId={}, activeSessions={}",
				chatroomId, session.getId(), countSessions(chatroomId));
	}

	public void unregister(UUID chatroomId, WebSocketSession session) {
		List<WebSocketSession> list = sessionsByChatroom.get(chatroomId);
		if (list != null) {
			list.remove(session);
			if (list.isEmpty()) {
				sessionsByChatroom.remove(chatroomId);
			}
		}
		log.debug("[WS] 세션 해제: chatroomId={}, sessionId={}, activeSessions={}",
				chatroomId, session.getId(), countSessions(chatroomId));
	}

	public void sendToChatroom(UUID chatroomId, String eventName, Object data) {
		List<WebSocketSession> list = sessionsByChatroom.get(chatroomId);
		if (list == null || list.isEmpty()) return;

		Object dataValue = (data != null) ? normalizeData(data) : Map.of();

		String payload;
		try {
			Map<String, Object> wrapper = Map.of("event", eventName, "data", dataValue);
			payload = objectMapper.writeValueAsString(wrapper);
		} catch (Exception e) {
			log.warn("[WS] 페이로드 직렬화 실패: chatroomId={}, eventName={}, error={}", chatroomId, eventName, e.getMessage());
			return;
		}

		TextMessage message = new TextMessage(payload);
		for (WebSocketSession session : list) {
			try {
				if (session.isOpen()) {
					session.sendMessage(message);
				}
			} catch (IOException e) {
				log.warn("[WS] 전송 실패: chatroomId={}, eventName={}, sessionId={}, error={}",
						chatroomId, eventName, session.getId(), e.getMessage(), e);
				unregister(chatroomId, session);
			}
		}
	}

	private Object normalizeData(Object data) {
		if (data instanceof Map<?, ?> map) {
			java.util.Map<String, Object> normalized = new java.util.HashMap<>();
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				String key = String.valueOf(entry.getKey());
				Object value = entry.getValue();
				if (value == null) {
					normalized.put(key, null);
				} else if (value instanceof String || value instanceof Number || value instanceof Boolean) {
					normalized.put(key, value);
				} else if (value instanceof LocalDateTime) {
					normalized.put(key, value.toString());
				} else if (value instanceof Map<?, ?>) {
					normalized.put(key, normalizeData(value));
				} else if (value instanceof java.util.List<?>) {
					java.util.List<?> list = (java.util.List<?>) value;
					java.util.List<Object> normalizedList = new java.util.ArrayList<>(list.size());
					for (Object item : list) {
						if (item == null || item instanceof String || item instanceof Number || item instanceof Boolean) {
							normalizedList.add(item);
						} else if (item instanceof LocalDateTime) {
							normalizedList.add(item.toString());
						} else if (item instanceof Map<?, ?> || item instanceof java.util.List<?>) {
							normalizedList.add(normalizeData(item));
						} else {
							normalizedList.add(item.toString());
						}
					}
					normalized.put(key, normalizedList);
				} else {
					normalized.put(key, value.toString());
				}
			}
			return normalized;
		}
		if (data instanceof java.util.List<?>) {
			java.util.List<?> list = (java.util.List<?>) data;
			java.util.List<Object> normalizedList = new java.util.ArrayList<>(list.size());
			for (Object item : list) {
				if (item == null || item instanceof String || item instanceof Number || item instanceof Boolean) {
					normalizedList.add(item);
				} else if (item instanceof LocalDateTime) {
					normalizedList.add(item.toString());
				} else if (item instanceof Map<?, ?> || item instanceof java.util.List<?>) {
					normalizedList.add(normalizeData(item));
				} else {
					normalizedList.add(item.toString());
				}
			}
			return normalizedList;
		}
		if (data instanceof LocalDateTime) {
			return data.toString();
		}
		return data;
	}

	public boolean hasSessions(UUID chatroomId) {
		return countSessions(chatroomId) > 0;
	}

	private int countSessions(UUID chatroomId) {
		List<WebSocketSession> list = sessionsByChatroom.get(chatroomId);
		return list != null ? list.size() : 0;
	}
}
