package com.dorandoran.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 핸드셰이크 시 X-User-Id 헤더(Gateway가 JWT 검증 후 주입)를 세션 attributes에 저장.
 * ChatWebSocketHandler에서 이를 사용하여 userId를 확보한다.
 */
@Component
@Slf4j
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

	public static final String ATTR_USER_ID = "userId";

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
		String userId = request.getHeaders().getFirst("X-User-Id");
		if (userId == null && request instanceof ServletServerHttpRequest servletRequest) {
			userId = servletRequest.getServletRequest().getHeader("X-User-Id");
		}
		if (userId != null && !userId.isBlank()) {
			attributes.put(ATTR_USER_ID, userId);
			log.debug("[WS] Handshake: X-User-Id 추출 완료: userId={}", userId);
			return true;
		}
		log.warn("[WS] Handshake 실패: X-User-Id 헤더 없음 (JWT 검증 필요)");
		return false;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception ex) {
		// no-op
	}
}
