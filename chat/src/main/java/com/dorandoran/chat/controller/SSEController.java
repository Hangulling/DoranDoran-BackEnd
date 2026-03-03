package com.dorandoran.chat.controller;

import com.dorandoran.chat.sse.SSEManager;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.service.ConnectionGreetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	private final ConnectionGreetingService connectionGreetingService;

	@GetMapping(value = "/stream/{chatroomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<SseEmitter> stream(@PathVariable UUID chatroomId,
	                                        @RequestParam(required = false) UUID userId,
	                                        @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		boolean likelyIos = isLikelyIos(userAgent);

		log.info("[SSE] 연결 시도: chatroomId={}, userId param={}, userAgent={}, isLikelyIos={}",
				chatroomId, userId, (userAgent != null ? userAgent.substring(0, Math.min(80, userAgent.length())) : "null"), likelyIos);

		// SecurityContext에서 우선 추출, 없으면 요청 파라미터
		UUID uid = extractUserIdFromSecurityContext();
		if (uid == null && userId != null) {
			uid = userId;
		}
		if (uid == null) {
			log.warn("[SSE] 연결 실패(400): userId 없음, chatroomId={}, userAgent={}, isLikelyIos={}",
					chatroomId, (userAgent != null ? userAgent.substring(0, Math.min(80, userAgent.length())) : "null"), likelyIos);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		// 채팅방 접근 권한 확인
		if (!chatRoomRepository.existsByUser_IdAndIdAndIsDeletedFalse(uid, chatroomId)) {
			log.warn("[SSE] 접근 거부(403): userId={}, chatroomId={}, userAgent={}, isLikelyIos={}",
					uid, chatroomId, (userAgent != null ? userAgent.substring(0, Math.min(80, userAgent.length())) : "null"), likelyIos);
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		// SSE 연결 생성
		SseEmitter emitter = sseManager.create(chatroomId);

		// 첫 연결 감지 및 AI 인사 발송
		connectionGreetingService.checkAndSendGreeting(chatroomId, uid);

		log.info("[SSE] 연결 성공: userId={}, chatroomId={}, userAgent={}, isLikelyIos={}",
				uid, chatroomId, (userAgent != null ? userAgent.substring(0, Math.min(80, userAgent.length())) : "null"), likelyIos);
		
		// CORS 헤더 추가 (SSE 스트림용 - Gateway에서 처리하지만 명시적으로 추가)
		// Gateway의 CORS 필터가 스트리밍 응답에도 적용되도록 함
		return ResponseEntity.ok()
				.header("Cache-Control", "no-cache")
				.header("Connection", "keep-alive")
				.header("X-Accel-Buffering", "no") // Nginx buffering 비활성화 (필요시)
				.body(emitter);
	}

	/**
	 * User-Agent로 iOS 기기 여부 추정 (iPhone, iPad, iPod, CFNetwork 등)
	 */
	private boolean isLikelyIos(String userAgent) {
		if (userAgent == null) return false;
		String ua = userAgent.toLowerCase();
		return ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod") || ua.contains("cfnetwork");
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
