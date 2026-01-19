package com.dorandoran.chat.sse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SSEManager {

	private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

	public SseEmitter create(UUID chatroomId) {
		SseEmitter emitter = new SseEmitter(0L);
		emitter.onCompletion(() -> remove(chatroomId, emitter));
		emitter.onTimeout(() -> remove(chatroomId, emitter));
		emitter.onError((ex) -> remove(chatroomId, emitter));
		emitters.computeIfAbsent(chatroomId, k -> new CopyOnWriteArrayList<>()).add(emitter);
		return emitter;
	}

	public boolean hasEmitters(UUID chatroomId) {
		List<SseEmitter> list = emitters.get(chatroomId);
		return list != null && !list.isEmpty();
	}

	public void send(UUID chatroomId, String eventName, Object data) {
		List<SseEmitter> list = emitters.get(chatroomId);
		if (list == null || list.isEmpty()) return;
		for (SseEmitter emitter : list) {
			try {
				// SSE에서는 JSON 객체를 문자열로 변환하여 전송
				String jsonData = (data instanceof String) ? (String) data : 
					(data instanceof Map) ? convertMapToJson((Map<?, ?>) data) : data.toString();
				emitter.send(SseEmitter.event().name(eventName).data(jsonData));
			} catch (IOException e) {
				remove(chatroomId, emitter);
			}
		}
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
