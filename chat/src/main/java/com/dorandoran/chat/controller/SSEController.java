package com.dorandoran.chat.controller;

import com.dorandoran.chat.sse.SSEManager;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.MessageRepository;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.dorandoran.chat.service.GreetingService;
import com.dorandoran.chat.enums.ChatRoomConcept;
import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.IntimacyProgress;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class SSEController {

	private final SSEManager sseManager;
	private final ChatRoomRepository chatRoomRepository;
	private final MessageRepository messageRepository;
	private final IntimacyProgressRepository intimacyProgressRepository;
	private final GreetingService greetingService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@GetMapping(value = "/stream/{chatroomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<SseEmitter> stream(@PathVariable UUID chatroomId, 
	                                        @RequestParam(required = false) UUID userId) {
		// SecurityContext에서 우선 추출, 없으면 요청 파라미터
		UUID uid = extractUserIdFromSecurityContext();
		if (uid == null && userId != null) {
			uid = userId;
		}
		if (uid == null) {
			log.warn("SSE 연결 실패: 사용자 ID 없음, chatroomId={}", chatroomId);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
		
		// 채팅방 접근 권한 확인
                if (!chatRoomRepository.existsByUser_IdAndIdAndIsDeletedFalse(uid, chatroomId)) {
			log.warn("SSE 접근 거부: userId={}, chatroomId={}", uid, chatroomId);
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		// SSE 연결 생성
		SseEmitter emitter = sseManager.create(chatroomId);
		
		// 첫 연결 감지 및 AI 인사 발송
		checkAndSendGreeting(chatroomId, uid);
		
		log.info("SSE 연결 성공: userId={}, chatroomId={}", uid, chatroomId);
		
		// CORS 헤더 추가 (SSE 스트림용 - Gateway에서 처리하지만 명시적으로 추가)
		// Gateway의 CORS 필터가 스트리밍 응답에도 적용되도록 함
		return ResponseEntity.ok()
				.header("Cache-Control", "no-cache")
				.header("Connection", "keep-alive")
				.header("X-Accel-Buffering", "no") // Nginx buffering 비활성화 (필요시)
				.body(emitter);
	}
	
	/**
	 * 첫 연결 감지 및 AI 인사 발송
	 */
	private void checkAndSendGreeting(UUID chatroomId, UUID userId) {
		// 비동기로 처리하여 SSE 연결 응답을 먼저 반환
		CompletableFuture.runAsync(() -> {
			try {
				// 1. intimacy_progress 테이블에 레코드가 있는지 확인
				Optional<IntimacyProgress> progressOpt = intimacyProgressRepository.findByChatRoomId(chatroomId);
				if (progressOpt.isEmpty()) {
					log.debug("IntimacyProgress 없음, 인사 발송 건너뜀: chatroomId={}", chatroomId);
					return;
				}
				
				// 2. messages 테이블에 bot 메시지가 있는지 확인
				boolean hasBotMessage = messageRepository.existsByChatRoomIdAndSenderType(chatroomId, "bot");
				if (hasBotMessage) {
					log.debug("이미 bot 메시지 존재, 인사 발송 건너뜀: chatroomId={}", chatroomId);
					return;
				}
				
				// 3. 채팅방 정보 조회
				ChatRoom chatRoom = chatRoomRepository.findById(chatroomId).orElse(null);
				if (chatRoom == null) {
					log.warn("채팅방을 찾을 수 없음: chatroomId={}", chatroomId);
					return;
				}
				
				// 4. concept과 intimacyLevel 추출
				String conceptStr = extractConceptFromSettings(chatRoom.getSettings());
				ChatRoomConcept concept = ChatRoomConcept.fromString(conceptStr);
				int intimacyLevel = progressOpt.get().getIntimacyLevel();
				
				log.info("첫 연결 감지, AI 인사 발송 시작: chatroomId={}, concept={}, intimacyLevel={}", 
					chatroomId, concept, intimacyLevel);
				
				// 5. AI 인사 발송
				greetingService.sendGreeting(chatroomId, userId, concept, intimacyLevel);
				
				// 6. SSE로 인사 메시지 전송
				sseManager.send(chatroomId, "greeting_message", Map.of(
					"type", "greeting",
					"message", "AI 인사말이 생성되었습니다."
				));
				
			} catch (Exception e) {
				log.error("AI 인사 발송 중 오류: chatroomId={}", chatroomId, e);
			}
		});
	}
	
	/**
	 * 채팅방 설정에서 concept 추출
	 */
	private String extractConceptFromSettings(JsonNode settings) {
		if (settings != null && settings.has("concept")) {
			return settings.get("concept").asText();
		}
		return "FRIEND"; // 기본값
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
