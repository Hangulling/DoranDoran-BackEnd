package com.dorandoran.chat.sse;

import com.dorandoran.chat.websocket.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class SSEManager {

	private final WebSocketSessionRegistry webSocketSessionRegistry;
	private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

	public SseEmitter create(UUID chatroomId) {
		SseEmitter emitter = new SseEmitter(0L);
		emitter.onCompletion(() -> {
			log.debug("[SSE] 연결 종료(completion): chatroomId={}, activeEmittersBeforeRemove={}", chatroomId, countEmitters(chatroomId));
			remove(chatroomId, emitter);
		});
		emitter.onTimeout(() -> {
			log.warn("[SSE] 연결 종료(timeout): chatroomId={}, activeEmittersBeforeRemove={}", chatroomId, countEmitters(chatroomId));
			remove(chatroomId, emitter);
		});
		emitter.onError((ex) -> {
			log.warn("[SSE] 연결 종료(error): chatroomId={}, error={}, activeEmittersBeforeRemove={}", chatroomId, ex.getMessage(), countEmitters(chatroomId), ex);
			remove(chatroomId, emitter);
		});
		emitters.computeIfAbsent(chatroomId, k -> new CopyOnWriteArrayList<>()).add(emitter);
		log.debug("[SSE] emitter 생성: chatroomId={}, activeEmitters={}", chatroomId, countEmitters(chatroomId));
		return emitter;
	}

	private int countEmitters(UUID chatroomId) {
		List<SseEmitter> list = emitters.get(chatroomId);
		return list != null ? list.size() : 0;
	}

	public boolean hasAnySubscribers(UUID chatroomId) {
		// SSE 또는 WebSocket 어느 한 쪽이라도 연결이 있으면 true
		List<SseEmitter> list = emitters.get(chatroomId);
		boolean hasSse = list != null && !list.isEmpty();
		boolean hasWs = webSocketSessionRegistry.hasSessions(chatroomId);
		return hasSse || hasWs;
	}

	public void send(UUID chatroomId, String eventName, Object data) {
		// SSE 클라이언트로 전송
		List<SseEmitter> list = emitters.get(chatroomId);
		if (list != null && !list.isEmpty()) {
			for (SseEmitter emitter : list) {
				try {
					String jsonData = (data instanceof String) ? (String) data :
						(data instanceof Map) ? convertMapToJson((Map<?, ?>) data) : data.toString();
					emitter.send(SseEmitter.event().name(eventName).data(jsonData));
				} catch (IOException e) {
					log.warn("[SSE] 전송 실패: chatroomId={}, eventName={}, error={}", chatroomId, eventName, e.getMessage(), e);
					remove(chatroomId, emitter);
				}
			}
		}
		// WebSocket 클라이언트로도 전송 (iOS 대응)
		webSocketSessionRegistry.sendToChatroom(chatroomId, eventName, data);
	}
	
	private String convertMapToJson(Map<?, ?> map) {
		try {
			// 간단한 JSON 변환 (실제로는 Jackson ObjectMapper 사용 권장)
			StringBuilder json = new StringBuilder("{");
			boolean first = true;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (!first) json.append(",");
				json.append("\"").append(entry.getKey()).append("\":");
				
				Object value = entry.getValue();
				if (value instanceof String) {
					json.append("\"").append(escapeJsonString((String) value)).append("\"");
				} else if (value instanceof Number || value instanceof Boolean) {
					json.append(value);
				} else if (value instanceof Map) {
					json.append(convertMapToJson((Map<?, ?>) value));
				} else if (value instanceof java.util.List) {
					json.append(convertListToJson((java.util.List<?>) value));
				} else {
					// ErrorResponse나 다른 복잡한 객체는 toString()으로 처리
					json.append("\"").append(escapeJsonString(value.toString())).append("\"");
				}
				first = false;
			}
			json.append("}");
			return json.toString();
		} catch (Exception e) {
			return "{\"error\":\"JSON conversion failed\"}";
		}
	}
	
	private String convertListToJson(java.util.List<?> list) {
		StringBuilder json = new StringBuilder("[");
		boolean first = true;
		for (Object item : list) {
			if (!first) json.append(",");
			if (item instanceof String) {
				json.append("\"").append(escapeJsonString((String) item)).append("\"");
			} else if (item instanceof Map) {
				json.append(convertMapToJson((Map<?, ?>) item));
			} else {
				json.append("\"").append(escapeJsonString(item.toString())).append("\"");
			}
			first = false;
		}
		json.append("]");
		return json.toString();
	}
	
	private String escapeJsonString(String str) {
		if (str == null) return "";
		return str.replace("\\", "\\\\")
		         .replace("\"", "\\\"")
		         .replace("\n", "\\n")
		         .replace("\r", "\\r")
		         .replace("\t", "\\t");
	}

	private void remove(UUID chatroomId, SseEmitter emitter) {
		List<SseEmitter> list = emitters.get(chatroomId);
		if (list != null) {
			list.remove(emitter);
			if (list.isEmpty()) {
				emitters.remove(chatroomId);
			}
		}
	}
}
