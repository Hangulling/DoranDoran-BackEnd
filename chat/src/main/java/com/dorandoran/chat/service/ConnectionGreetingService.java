package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.enums.ChatRoomConcept;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.dorandoran.chat.repository.MessageRepository;
import com.dorandoran.chat.sse.SSEManager;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SSE 또는 WebSocket 첫 연결 시 AI 인사 발송을 트리거합니다.
 * SSEController와 ChatWebSocketHandler에서 공통으로 사용합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionGreetingService {

	private final IntimacyProgressRepository intimacyProgressRepository;
	private final MessageRepository messageRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final GreetingService greetingService;
	private final SSEManager sseManager;

	/**
	 * 첫 연결 감지 시 AI 인사 발송 (필요한 경우에만).
	 * 비동기로 처리되어 호출자에게 즉시 반환됩니다.
	 */
	public void checkAndSendGreeting(UUID chatroomId, UUID userId) {
		CompletableFuture.runAsync(() -> {
			try {
				Optional<IntimacyProgress> progressOpt = intimacyProgressRepository.findByChatRoomId(chatroomId);
				if (progressOpt.isEmpty()) {
					log.debug("IntimacyProgress 없음, 인사 발송 건너뜀: chatroomId={}", chatroomId);
					return;
				}
				boolean hasBotMessage = messageRepository.existsByChatRoomIdAndSenderType(chatroomId, "bot");
				if (hasBotMessage) {
					log.debug("이미 bot 메시지 존재, 인사 발송 건너뜀: chatroomId={}", chatroomId);
					return;
				}
				ChatRoom chatRoom = chatRoomRepository.findById(chatroomId).orElse(null);
				if (chatRoom == null) {
					log.warn("채팅방을 찾을 수 없음: chatroomId={}", chatroomId);
					return;
				}
				String conceptStr = extractConceptFromSettings(chatRoom.getSettings());
				ChatRoomConcept concept = ChatRoomConcept.fromString(conceptStr);
				int intimacyLevel = progressOpt.get().getIntimacyLevel();
				log.info("첫 연결 감지, AI 인사 발송 시작: chatroomId={}, concept={}, intimacyLevel={}",
						chatroomId, concept, intimacyLevel);
				greetingService.sendGreeting(chatroomId, userId, concept, intimacyLevel);
				sseManager.send(chatroomId, "greeting_message", Map.of(
						"type", "greeting",
						"message", "AI 인사말이 생성되었습니다."
				));
			} catch (Exception e) {
				log.error("AI 인사 발송 중 오류: chatroomId={}", chatroomId, e);
			}
		});
	}

	private String extractConceptFromSettings(JsonNode settings) {
		if (settings != null && settings.has("concept")) {
			JsonNode conceptNode = settings.get("concept");
			if (conceptNode.isTextual()) {
				return conceptNode.asText().toUpperCase();
			}
			log.warn("ConnectionGreetingService: concept이 유효하지 않은 타입입니다. 기본값 FRIEND 사용. type={}",
					conceptNode.getNodeType());
			return "FRIEND";
		}
		return "FRIEND";
	}
}
