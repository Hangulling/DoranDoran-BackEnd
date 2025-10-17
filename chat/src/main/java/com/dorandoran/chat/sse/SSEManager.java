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

	public void send(UUID chatroomId, String eventName, Object data) {
		List<SseEmitter> list = emitters.get(chatroomId);
		if (list == null || list.isEmpty()) return;
		for (SseEmitter emitter : list) {
			try {
				emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
			} catch (IOException e) {
				remove(chatroomId, emitter);
			}
		}
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
