package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.entity.User;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.event.MessageEvent;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.MessageRepository;
import com.dorandoran.chat.repository.UserRepository;
import com.dorandoran.chat.repository.ChatbotRepository;
import com.dorandoran.chat.messaging.RedisMessagePublisher;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * Chat Service (Redis Pub/Sub + 캐싱 통합)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatbotRepository chatbotRepository;
    private final IntimacyProgressRepository intimacyProgressRepository;
    private final ObjectMapper objectMapper;
    // AI 트리거는 컨트롤러에서 수행하여 순환 의존 제거
    private final RedisMessagePublisher redisPublisher;
    private final RedisCacheService cacheService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 채팅방 조회 또는 생성 (userId + chatbotId 조합)
     */
    @Transactional
    public ChatRoom getOrCreateRoom(UUID userId, UUID chatbotId, String name) {
        return getOrCreateRoom(userId, chatbotId, name, "FRIEND", 2);
    }

    /**
     * 채팅방 조회 또는 생성 (컨셉과 친밀도 포함)
     */
    @Transactional
    public ChatRoom getOrCreateRoom(UUID userId, UUID chatbotId, String name, String concept, Integer intimacyLevel) {
        // 먼저 삭제 여부 무관하게 채팅방 조회
        Optional<ChatRoom> existing = chatRoomRepository.findByUserIdAndChatbotId(userId, chatbotId);
        if (existing.isPresent()) {
            ChatRoom room = existing.get();

            // 삭제된 채팅방인 경우 복구
            if (room.getIsDeleted()) {
                room.setIsDeleted(false);
                room.setUpdatedAt(LocalDateTime.now());
                chatRoomRepository.save(room);
            }

            // 기존 채팅방의 concept과 intimacyLevel 업데이트
            updateRoomSettings(room, concept);
            updateIntimacyLevel(room.getId(), userId, intimacyLevel);
            return room;
        }

        // User와 Chatbot 객체 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Chatbot chatbot = chatbotRepository.findById(chatbotId)
            .orElseThrow(() -> new RuntimeException("Chatbot not found: " + chatbotId));

        // UUID 충돌 방지: 기존 레코드와 겹치지 않을 때까지 생성
        UUID roomId;
        do {
            roomId = UUID.randomUUID();
        } while (chatRoomRepository.findById(roomId).isPresent());

        // settings에 concept 저장
        ObjectNode settings = objectMapper.createObjectNode();
        settings.put("concept", concept);

        ChatRoom room = ChatRoom.builder()
            .id(roomId)
            .user(user)
            .chatbot(chatbot)
            .name(name)
            .settings(settings)
            .isArchived(false)
            .isDeleted(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        ChatRoom savedRoom = chatRoomRepository.save(room);

        // 새로 생성된 채팅방 캐싱
        cacheService.cacheChatRoom(savedRoom);

        // IntimacyProgress 초기화
        initializeIntimacyProgress(savedRoom.getId(), userId, intimacyLevel);

        return savedRoom;
    }

    /**
     * 다음 시퀀스 번호 계산 (채팅방 내 최대값 + 1)
     */
    @Transactional
    public long nextSequenceNumber(UUID chatroomId) {
        return messageRepository.findTopByChatRoomIdOrderBySequenceNumberDesc(chatroomId)
            .map(m -> m.getSequenceNumber() + 1)
            .orElse(1L);
    }

//    /**
//     * 메시지 전송
//     */
//    @Transactional
//    public Message sendMessage(UUID chatroomId, UUID senderId, String senderType, String content, String contentType) {
//        long seq = nextSequenceNumber(chatroomId);
//        // ChatRoom 객체 조회
//        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
//            .orElseThrow(() -> new RuntimeException("ChatRoom not found: " + chatroomId));
//
//        Message message = Message.builder()
//            .id(UUID.randomUUID())
//            .chatRoom(chatRoom)
//            .senderType(senderType)
//            .senderId(senderId)
//            .content(content)
//            .contentType(contentType)
//            .sequenceNumber(seq)
//            .isDeleted(false)
//            .isEdited(false)
//            .build();
//        Message saved = messageRepository.save(message);
//
//        chatRoomRepository.findById(chatroomId).ifPresent(room -> {
//            room.setLastMessageAt(LocalDateTime.now());
//            room.setLastMessage(saved);
//            room.setUpdatedAt(LocalDateTime.now());
//            chatRoomRepository.save(room);
//
//            // 채팅방 캐시 무효화 (lastMessage 변경)
//            cacheService.invalidateChatRoomCache(chatroomId);
//        });
//
//        // 메시지 캐시에 추가
//        cacheService.appendMessageToCache(chatroomId, saved);
//
//        // Redis Pub/Sub으로 메시지 이벤트 발행
//        MessageEvent event = new MessageEvent(
//            saved.getId(),
//            chatroomId,
//            senderId,
//            senderType,
//            content,
//            contentType,
//            System.currentTimeMillis()
//        );
//        redisPublisher.publishMessage(event);
//
//        log.debug("메시지 저장 및 발행 완료: messageId={}, chatroomId={}", saved.getId(), chatroomId);
//
//        return saved;
//    }

    /**
     * 임시 메시지 생성 (Redis 전용, messages 테이블 저장 안 함)
     * 보관함에 저장하기 전까지는 Redis에만 보관
     */
    @Transactional
    public Message sendTemporaryMessage(UUID chatroomId, UUID senderId, String senderType, String content, String contentType) {
        long seq = nextSequenceNumberFromRedis(chatroomId);

        Message message = Message.builder()
            .id(UUID.randomUUID())
            .chatRoom(ChatRoom.builder().id(chatroomId).build())  // Proxy 객체 (DB 조회 안 함)
            .senderType(senderType)
            .senderId(senderId)
            .content(content)
            .contentType(contentType)
            .sequenceNumber(seq)
            .isDeleted(false)
            .isEdited(false)
            .createdAt(LocalDateTime.now())
            .build();

        // Redis에만 저장 (messages 테이블 저장 안 함)
        cacheService.appendTemporaryMessage(chatroomId, message);

        // 채팅방 lastMessage 업데이트 (DB)
        chatRoomRepository.findById(chatroomId).ifPresent(room -> {
            room.setLastMessageAt(LocalDateTime.now());
            /**
             * 임시 메시지는 messages 테이블에 없으므로 외래키 제약 위반
             * 옵션 1: last_message_id는 NULL로 두고, last_message_at만 업데이트 * 현재 적용
             * 옵션 2: 외래키 제약 조건 수정 (DEFERRABLE 또는 제거)
             * 옵션 3: Redis에 last_message 정보 별도 저장
             */
            room.setLastMessage(null);  // 임시 메시지는 외래키 제약으로 인해 NULL
            room.setUpdatedAt(LocalDateTime.now());
            chatRoomRepository.save(room);

            // 채팅방 캐시 무효화
            cacheService.invalidateChatRoomCache(chatroomId);
        });

        // Redis Pub/Sub 발행
        MessageEvent event = new MessageEvent(
            message.getId(),
            chatroomId,
            senderId,
            senderType,
            content,
            contentType,
            System.currentTimeMillis()
        );
        redisPublisher.publishMessage(event);

        log.debug("임시 메시지 생성 (Redis only): messageId={}, chatroomId={}", message.getId(), chatroomId);
        return message;
    }

    /**
     * Redis 기반 시퀀스 번호 생성 (임시 메시지용)
     */
    private long nextSequenceNumberFromRedis(UUID chatroomId) {
        String key = "seq:" + chatroomId;
        Long seq = redisTemplate.opsForValue().increment(key);

        if (seq == null || seq == 1) {
            // 초기화: DB에서 최대 시퀀스 번호 조회
            Long maxSeq = messageRepository.findTopByChatRoomIdOrderBySequenceNumberDesc(chatroomId)
                .map(Message::getSequenceNumber)
                .orElse(0L);

            // Redis에 초기값 설정
            redisTemplate.opsForValue().set(key, maxSeq + 1);

            // TTL 설정 (1일)
            redisTemplate.expire(key, Duration.ofDays(1));

            log.debug("Redis 시퀀스 초기화: chatroomId={}, startSeq={}", chatroomId, maxSeq + 1);
            return maxSeq + 1;
        }

        log.debug("Redis 시퀀스 생성: chatroomId={}, seq={}", chatroomId, seq);
        return seq;
    }

    /**
     * 사용자별 채팅방 목록 조회 (삭제되지 않은, 최신 메시지 순) - 페이징
     */
    @Transactional
    public Page<ChatRoom> listRooms(UUID userId, Pageable pageable) {
        return chatRoomRepository.findByUserIdAndIsDeletedFalseOrderByLastMessageAtDesc(userId, pageable);
    }

    /**
     * 사용자별 채팅방 목록 조회 (삭제되지 않은, 최신 메시지 순) - 전체
     */
    @Transactional
    public List<ChatRoom> listRooms(UUID userId) {
        return chatRoomRepository.findByUserIdAndIsDeletedFalseOrderByLastMessageAtDesc(userId);
    }

    /**
     * 채팅방 메시지 목록 조회 (시퀀스 오름차순) - 페이징
     */
    @Transactional
    public Page<Message> listMessages(UUID chatroomId, Pageable pageable) {
        return messageRepository.findByChatRoomIdOrderBySequenceNumberAsc(chatroomId, pageable);
    }

//    /**
//     * 채팅방 메시지 목록 조회 (시퀀스 오름차순) - 전체
//     */
//    @Transactional
//    public List<Message> listMessages(UUID chatroomId) {
//        // 1. 캐시 확인
//        List<Message> cached = cacheService.getCachedMessages(chatroomId);
//        if (!cached.isEmpty()) {
//            log.debug("메시지 캐시에서 반환: chatroomId={}, count={}", chatroomId, cached.size());
//            return cached;
//        }
//
//        // 2. DB 조회
//        log.debug("메시지 DB 조회: chatroomId={}", chatroomId);
//        List<Message> messages = messageRepository.findByChatRoomIdOrderBySequenceNumberAsc(chatroomId);
//
//        // 3. 캐시 저장
//        if (!messages.isEmpty()) {
//            cacheService.cacheMessages(chatroomId, messages);
//        }
//
//        return messages;
//    }

    @Transactional
    public List<Message> listMessages(UUID chatroomId) {
        List<Message> result = new ArrayList<>();

        // 1. Redis 임시 메시지 조회
        List<Message> temporaryMessages = cacheService.getTemporaryMessages(chatroomId);
        log.debug("Redis 임시 메시지 조회: chatroomId={}, count={}", chatroomId, temporaryMessages.size());

        // 2. DB 영구 메시지 조회 (캐시 우선)
        List<Message> cachedMessages = cacheService.getCachedMessages(chatroomId);
        if (!cachedMessages.isEmpty()) {
            log.debug("영구 메시지 캐시 히트: chatroomId={}, count={}", chatroomId, cachedMessages.size());
            result.addAll(cachedMessages);
        } else {
            // isDeleted = false인 메시지만 조회
            List<Message> savedMessages = messageRepository.findByChatRoomIdAndIsDeletedFalseOrderBySequenceNumberAsc(chatroomId);
            log.debug("영구 메시지 DB 조회: chatroomId={}, count={}", chatroomId, savedMessages.size());
            result.addAll(savedMessages);

            if (!savedMessages.isEmpty()) {
                cacheService.cacheMessages(chatroomId, savedMessages);
            }
        }

        // 3. 임시 메시지 추가
        result.addAll(temporaryMessages);

        // 4. 시퀀스 번호로 정렬
        result.sort(Comparator.comparing(Message::getSequenceNumber));

        log.debug("전체 메시지 조회 완료: chatroomId={}, total={} (임시={}, 영구={})",
            chatroomId, result.size(), temporaryMessages.size(), result.size() - temporaryMessages.size());

        return result;
    }

    /**
     * 채팅방 조회 (ID로)
     */
    @Transactional
    public ChatRoom getChatRoomById(UUID chatroomId) {
        // 1. 캐시 확인
        ChatRoom cached = cacheService.getCachedChatRoom(chatroomId);
        if (cached != null) {
            log.debug("채팅방 캐시에서 반환: chatroomId={}", chatroomId);
            return cached;
        }

        // 2. DB 조회
        log.debug("채팅방 DB 조회: chatroomId={}", chatroomId);
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
            .orElseThrow(() -> new RuntimeException("ChatRoom not found: " + chatroomId));

        // 3. 캐시 저장
        cacheService.cacheChatRoom(chatRoom);

        return chatRoom;
    }

//    /**
//     * 채팅방 수정 (이름/설명/아카이브)
//     */
//    @Transactional
//    public ChatRoom updateRoom(UUID chatroomId, UUID userId, String name, String description, Boolean archived) {
//        ChatRoom room = getChatRoomById(chatroomId);
//        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
//            throw new RuntimeException("Access denied or room deleted: " + chatroomId);
//        }
//        if (name != null && !name.isBlank()) {
//            room.setName(name);
//        }
//        if (description != null) {
//            room.setDescription(description);
//        }
//        if (archived != null) {
//            room.setIsArchived(archived);
//        }
//        room.setUpdatedAt(java.time.LocalDateTime.now());
//        return chatRoomRepository.save(room);
//    }

    /**
     * 채팅방 수정 (이름/설명/아카이브)
     */
    @Transactional
    public ChatRoom updateRoom(UUID chatroomId, UUID userId, String name, String description, Boolean archived) {
        ChatRoom room = getChatRoomById(chatroomId);
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
            throw new RuntimeException("Access denied or room deleted: " + chatroomId);
        }

        boolean statusChanged = false;
        String statusType = null;

        if (name != null && !name.isBlank()) {
            room.setName(name);
        }
        if (description != null) {
            room.setDescription(description);
        }
        if (archived != null && !archived.equals(room.getIsArchived())) {
            room.setIsArchived(archived);
            statusChanged = true;
            statusType = archived ? "archived" : "unarchived";
        }
        room.setUpdatedAt(java.time.LocalDateTime.now());
        ChatRoom saved = chatRoomRepository.save(room);

        // 캐시 무효화
        cacheService.invalidateChatRoomCache(chatroomId);

        // 상태 변경 이벤트 발행
        if (statusChanged) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("roomName", saved.getName());
            metadata.put("userId", userId);
            redisPublisher.publishRoomStatusChangeEvent(chatroomId, statusType, metadata);
        }

        return saved;
    }

//    /**
//     * 채팅방 소프트 삭제
//     */
//    @Transactional
//    public void softDeleteRoom(UUID chatroomId, UUID userId) {
//        ChatRoom room = getChatRoomById(chatroomId);
//        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
//            throw new RuntimeException("Access denied or room already deleted: " + chatroomId);
//        }
//        room.setIsDeleted(true);
//        room.setUpdatedAt(java.time.LocalDateTime.now());
//        chatRoomRepository.save(room);
//    }

    /**
     * 채팅방 소프트 삭제
     */
    @Transactional
    public void softDeleteRoom(UUID chatroomId, UUID userId) {
        ChatRoom room = getChatRoomById(chatroomId);
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
            throw new RuntimeException("Access denied or room already deleted: " + chatroomId);
        }

        // 보관함에 저장되지 않은 메시지를 캐시로 이동
        List<Message> messages = listMessages(chatroomId);
        cacheService.cacheUnsavedMessages(chatroomId, messages);

        room.setIsDeleted(true);
        room.setUpdatedAt(java.time.LocalDateTime.now());
        chatRoomRepository.save(room);

        // 캐시 무효화
        cacheService.invalidateChatRoomCache(chatroomId);
        cacheService.invalidateMessageCache(chatroomId);

        // 상태 변경 이벤트 발행
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("roomName", room.getName());
        metadata.put("userId", userId);
        redisPublisher.publishRoomStatusChangeEvent(chatroomId, "deleted", metadata);

        log.info("채팅방 소프트 삭제 완료: chatroomId={}, 메시지 {}개 캐시 보관", chatroomId, messages.size());
    }

    /**
     * 코치마크 표시 여부 조회 (room.settings.coachmarkShown)
     */
    @Transactional
    public boolean isCoachmarkShown(UUID chatroomId, UUID userId) {
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
            throw new RuntimeException("Access denied or room deleted: " + chatroomId);
        }
        ChatRoom room = getChatRoomById(chatroomId);
        JsonNode settings = room.getSettings();
        if (settings != null && settings.has("coachmarkShown")) {
            return settings.get("coachmarkShown").asBoolean(false);
        }
        return false;
    }

    /**
     * 코치마크 표시 완료로 설정 (room.settings.coachmarkShown=true)
     */
    @Transactional
    public ChatRoom setCoachmarkShown(UUID chatroomId, UUID userId, boolean shown) {
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
            throw new RuntimeException("Access denied or room deleted: " + chatroomId);
        }
        ChatRoom room = getChatRoomById(chatroomId);
        ObjectNode settings = room.getSettings() != null && room.getSettings().isObject()
            ? (ObjectNode) room.getSettings()
            : objectMapper.createObjectNode();
        settings.put("coachmarkShown", shown);
        room.setSettings(settings);
        room.setUpdatedAt(java.time.LocalDateTime.now());
        return chatRoomRepository.save(room);
    }

//    /**
//     * 친밀도 레벨 업데이트
//     */
//    @Transactional
//    public void updateIntimacyLevel(UUID chatroomId, UUID userId, int intimacyLevel) {
//        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
//            throw new RuntimeException("Access denied or room deleted: " + chatroomId);
//        }
//
//        IntimacyProgress progress = intimacyProgressRepository.findByChatRoomId(chatroomId)
//            .orElseGet(() -> {
//                ChatRoom chatRoom = getChatRoomById(chatroomId);
//                return IntimacyProgress.builder()
//                    .id(UUID.randomUUID())
//                    .chatRoom(chatRoom)
//                    .userId(userId)
//                    .intimacyLevel(intimacyLevel)
//                    .totalCorrections(0)
//                    .build();
//            });
//
//        progress.setIntimacyLevel(intimacyLevel);
//        progress.setLastUpdated(LocalDateTime.now());
//        intimacyProgressRepository.save(progress);
//    }

    /**
     * 친밀도 레벨 업데이트
     */
    @Transactional
    public void updateIntimacyLevel(UUID chatroomId, UUID userId, int intimacyLevel) {
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
            throw new RuntimeException("Access denied or room deleted: " + chatroomId);
        }

        IntimacyProgress progress = intimacyProgressRepository.findByChatRoomId(chatroomId)
            .orElseGet(() -> {
                ChatRoom chatRoom = getChatRoomById(chatroomId);
                return IntimacyProgress.builder()
                    .id(UUID.randomUUID())
                    .chatRoom(chatRoom)
                    .userId(userId)
                    .intimacyLevel(intimacyLevel)
                    .totalCorrections(0)
                    .build();
            });

        progress.setIntimacyLevel(intimacyLevel);
        progress.setLastUpdated(LocalDateTime.now());
        intimacyProgressRepository.save(progress);

        // 친밀도 캐싱
        cacheService.cacheIntimacyLevel(chatroomId, intimacyLevel);

        log.debug("친밀도 업데이트 및 캐싱 완료: chatroomId={}, level={}", chatroomId, intimacyLevel);
    }

    /**
     * IntimacyProgress 초기화
     */
    private void initializeIntimacyProgress(UUID chatroomId, UUID userId, int intimacyLevel) {
        IntimacyProgress progress = IntimacyProgress.builder()
            .id(UUID.randomUUID())
            .chatRoom(getChatRoomById(chatroomId))
            .userId(userId)
            .intimacyLevel(intimacyLevel)
            .totalCorrections(0)
            .lastFeedback("채팅방 생성")
            .lastUpdated(LocalDateTime.now())
            .progressData("{}")
            .build();

        intimacyProgressRepository.save(progress);
    }

    /**
     * 채팅방의 컨셉과 친밀도 레벨 조회
     */
    public String getConcept(UUID chatroomId) {
        ChatRoom room = getChatRoomById(chatroomId);
        JsonNode settings = room.getSettings();
        if (settings != null && settings.has("concept")) {
            return settings.get("concept").asText();
        }
        return "FRIEND"; // 기본값
    }

//    public Integer getIntimacyLevel(UUID chatroomId) {
//        return intimacyProgressRepository.findByChatRoomId(chatroomId)
//            .map(IntimacyProgress::getIntimacyLevel)
//            .orElse(2); // 기본값
//    }

    /**
     * 친밀도 정보 확인
     * @param chatroomId
     * @return IntimacyLevel
     */
    public Integer getIntimacyLevel(UUID chatroomId) {
        // 1. 캐시 확인
        Integer cached = cacheService.getIntimacyLevel(chatroomId);
        if (cached != null) {
            log.debug("친밀도 캐시에서 반환: chatroomId={}, level={}", chatroomId, cached);
            return cached;
        }

        // 2. DB 조회
        Integer level = intimacyProgressRepository.findByChatRoomId(chatroomId)
            .map(IntimacyProgress::getIntimacyLevel)
            .orElse(2); // 기본값

        // 3. 캐싱
        cacheService.cacheIntimacyLevel(chatroomId, level);

        return level;
    }

    /**
     * 채팅방 settings에 concept 저장
     */
    private void updateRoomSettings(ChatRoom room, String concept) {
        ObjectNode settings = room.getSettings() != null && room.getSettings().isObject()
            ? (ObjectNode) room.getSettings()
            : objectMapper.createObjectNode();
        settings.put("concept", concept);
        room.setSettings(settings);
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);
    }

    /**
     * 이메일로 사용자 조회
     */
    public com.dorandoran.chat.entity.User findUserByEmail(String email) {
        // user_schema.app_user에서 조회
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * concept별 챗봇 UUID 매핑
     */
    public UUID getChatbotIdByConcept(String concept) {
        switch (concept.toUpperCase()) {
            case "FRIEND":
                return UUID.fromString("22222222-2222-2222-2222-222222222221");
            case "HONEY":
                return UUID.fromString("22222222-2222-2222-2222-222222222222");
            case "COWORKER":
                return UUID.fromString("22222222-2222-2222-2222-222222222223");
            case "SENIOR":
                return UUID.fromString("22222222-2222-2222-2222-222222222224");
            default:
                return UUID.fromString("22222222-2222-2222-2222-222222222221"); // 기본값: FRIEND
        }
    }

    /**
     * 특정 메시지를 보관함에 저장 (Redis → DB 이동)
     * Store 서비스에서 호출
     */
    @Transactional
    public Message saveMessageToArchive(UUID chatroomId, UUID messageId, UUID userId) {
        // 1. 권한 확인
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
            throw new RuntimeException("Access denied or room deleted: " + chatroomId);
        }

        // 2. 이미 DB에 저장되어 있는지 확인
        Optional<Message> existingMessage = messageRepository.findById(messageId);
        if (existingMessage.isPresent()) {
            Message existing = existingMessage.get();
            log.debug("메시지가 이미 DB에 존재함: messageId={}", messageId);
            return existing;
        }

        // 3. Redis에서 임시 메시지 조회
        Message temporaryMessage = cacheService.getTemporaryMessage(chatroomId, messageId);
        if (temporaryMessage == null) {
            throw new RuntimeException("Message not found in Redis: " + messageId);
        }

        // 4. ChatRoom 엔티티 로드 및 설정
        ChatRoom chatRoom = getChatRoomById(chatroomId);
        temporaryMessage.setChatRoom(chatRoom);
        temporaryMessage.setIsDeleted(false);
        temporaryMessage.setIsEdited(false);

        // 5. DB에 저장
        Message savedMessage = messageRepository.save(temporaryMessage);

        // 6. 영구 메시지 캐시에 추가
        cacheService.appendMessageToCache(chatroomId, savedMessage);

        log.info("메시지 보관함 저장 완료: messageId={}, chatroomId={}", messageId, chatroomId);

        return savedMessage;
    }

    /**
     * 메시지 소프트 삭제 - 보관함 해제 시
     * Store 서비스에서 호출
     */
    @Transactional
    public void deleteMessageFromArchive(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        // 권한 확인: 메시지가 속한 채팅방의 소유자인지 확인
        ChatRoom chatRoom = message.getChatRoom();
        if (!chatRoom.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied: " + messageId);
        }

        // 이미 삭제됨
        if (message.getIsDeleted()) {
            log.warn("이미 삭제된 메시지: messageId={}", messageId);
            return;
        }

        // 소프트 삭제
        message.setIsDeleted(true);
        message.setDeletedAt(LocalDateTime.now());
        messageRepository.save(message);

        // 메시지 캐시 무효화
        cacheService.invalidateMessageCache(chatRoom.getId());

        log.info("메시지 소프트 삭제 완료: messageId={}, userId={}", messageId, userId);
    }
}