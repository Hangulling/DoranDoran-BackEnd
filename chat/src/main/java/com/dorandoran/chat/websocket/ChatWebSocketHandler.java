package com.dorandoran.chat.websocket;

import com.dorandoran.chat.service.ChatService;
import com.dorandoran.chat.service.AIService;
import com.dorandoran.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

	private final ChatService chatService;
	private final ChatRoomRepository chatRoomRepository;
	private final AIService aiService;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		UUID chatroomId = extractChatroomId(session.getUri());
		UUID userId = extractUserIdFromSession(session);
		
		if (chatroomId == null || userId == null) {
			log.warn("WebSocket 연결 실패: chatroomId={}, userId={}", chatroomId, userId);
			session.close(CloseStatus.BAD_DATA);
			return;
		}
		
		// 채팅방 접근 권한 확인
		if (!hasAccessToChatroom(userId, chatroomId)) {
			log.warn("WebSocket 접근 거부: userId={}, chatroomId={}", userId, chatroomId);
			session.close(CloseStatus.NOT_ACCEPTABLE);
			return;
		}
		
		log.info("WebSocket 연결 성공: userId={}, chatroomId={}", userId, chatroomId);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		UUID chatroomId = extractChatroomId(session.getUri());
		UUID userId = extractUserIdFromSession(session);
		
		if (chatroomId == null || userId == null) {
			log.warn("WebSocket 메시지 처리 실패: chatroomId={}, userId={}", chatroomId, userId);
			return;
		}
		
		// 채팅방 접근 권한 재확인
		if (!hasAccessToChatroom(userId, chatroomId)) {
			log.warn("WebSocket 메시지 접근 거부: userId={}, chatroomId={}", userId, chatroomId);
			return;
		}
		
		// 매우 단순한 포맷: senderId|senderType|content
		String payload = message.getPayload();
		String[] parts = payload.split("\\|", 3);
		if (parts.length < 3) {
			log.warn("잘못된 메시지 형식: {}", payload);
			return;
		}
		
		try {
			UUID senderId = UUID.fromString(parts[0]);
			String senderType = parts[1];
			String content = parts[2];
			
			// 발신자 ID 검증 (사용자만 허용)
			if (!userId.equals(senderId) || !"user".equals(senderType)) {
				log.warn("잘못된 발신자: userId={}, senderId={}, senderType={}", userId, senderId, senderType);
				return;
			}
			
//			var saved = chatService.sendMessage(chatroomId, senderId, senderType, content, "text");
			var saved = chatService.sendTemporaryMessage(chatroomId, senderId, senderType, content, "text");
			aiService.streamAIResponse(saved);
			log.debug("WebSocket 메시지 처리 완료: chatroomId={}, userId={}", chatroomId, userId);
		} catch (IllegalArgumentException e) {
			log.warn("잘못된 UUID 형식: {}", parts[0]);
		} catch (Exception e) {
			log.error("WebSocket 메시지 처리 중 오류", e);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		UUID chatroomId = extractChatroomId(session.getUri());
		UUID userId = extractUserIdFromSession(session);
		log.info("WebSocket 연결 종료: userId={}, chatroomId={}, status={}", userId, chatroomId, status);
	}

	private UUID extractChatroomId(URI uri) {
		if (uri == null) return null;
		// 경로 형식: /ws/chat/{chatroomId}
		String path = uri.getPath();
		String[] segments = path.split("/");
		if (segments.length >= 4) {
			try {
				return UUID.fromString(segments[3]);
			} catch (IllegalArgumentException ignored) {}
		}
		return null;
	}
	
	private UUID extractUserIdFromSession(WebSocketSession session) {
		// WebSocket 연결 시 쿼리 파라미터나 헤더에서 userId 추출
		// 예: /ws/chat/{chatroomId}?userId={userId}
		URI uri = session.getUri();
		if (uri != null && uri.getQuery() != null) {
			String query = uri.getQuery();
			String[] params = query.split("&");
			for (String param : params) {
				if (param.startsWith("userId=")) {
					try {
						return UUID.fromString(param.substring(7));
					} catch (IllegalArgumentException ignored) {}
				}
			}
		}
		return null;
	}
	
	private boolean hasAccessToChatroom(UUID userId, UUID chatroomId) {
		return chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId);
	}
}
