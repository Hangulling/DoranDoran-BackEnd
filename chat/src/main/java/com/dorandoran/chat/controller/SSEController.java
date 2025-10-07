package com.dorandoran.chat.controller;

import com.dorandoran.chat.sse.SSEManager;
import com.dorandoran.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class SSEController {

	private final SSEManager sseManager;
	private final ChatRoomRepository chatRoomRepository;

	@GetMapping(value = "/stream/{chatroomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<SseEmitter> stream(@PathVariable UUID chatroomId) {
		// 사용자 인증 확인
		UUID userId = extractUserIdFromSecurityContext();
		if (userId == null) {
			log.warn("SSE 연결 실패: 인증되지 않은 사용자, chatroomId={}", chatroomId);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		// 채팅방 접근 권한 확인
		if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
			log.warn("SSE 접근 거부: userId={}, chatroomId={}", userId, chatroomId);
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		log.info("SSE 연결 성공: userId={}, chatroomId={}", userId, chatroomId);
		return ResponseEntity.ok(sseManager.create(chatroomId));
	}
	
	private UUID extractUserIdFromSecurityContext() {
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UUID u) {
				return u;
			}
		} catch (Exception e) {
			log.debug("SecurityContext에서 userId 추출 실패", e);
		}
		return null;
	}
}
