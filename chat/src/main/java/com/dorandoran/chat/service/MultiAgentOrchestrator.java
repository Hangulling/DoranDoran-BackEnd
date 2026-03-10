package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.client.UserServiceClient;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.sse.SSEManager;
import com.dorandoran.chat.service.agent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Multi-Agent Orchestrator
 * Parallel + Aggregator 패턴으로 여러 Agent를 조율
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiAgentOrchestrator {
    private final IntimacyAgent intimacyAgent;
    private final VocabularyAgent vocabularyAgent;
    private final ConversationAgent conversationAgent;
    private final SummarizerAgent summarizerAgent;
    private final SSEManager sseManager;
    private final IntimacyProgressRepository intimacyProgressRepository;
    private final ChatService chatService;
    private final ChatRoomRepository chatRoomRepository;
    private final UserServiceClient userServiceClient;
    
    public void processUserMessage(UUID chatroomId, UUID userId, Message userMessage) {
        log.info("=== MultiAgentOrchestrator.processUserMessage() 호출됨 ===");
        String content = userMessage.getContent();

        if (shouldAbort(chatroomId, userMessage.getId(), userId)) {
            log.info("메시지 취소로 처리 중단: messageId={}", userMessage.getId());
            sseManager.send(chatroomId, "conversation_cancelled", Map.of("messageId", userMessage.getId()));
            return;
        }
        
        // 기존 Multi-Agent 로직 실행
        processWithAgents(chatroomId, userId, userMessage, content);
    }
    
    
    /**
     * 기존 Multi-Agent 처리 로직
     */
    private void processWithAgents(UUID chatroomId, UUID userId, Message userMessage, String content) {
        log.info("Multi-Agent 처리 시작: chatroomId={}, userId={}", chatroomId, userId);
        
        // Phase 1: 병렬 실행 (Intimacy, Vocabulary, Conversation)
        log.info("=== Phase 1: Parallel execution started ===");
        log.info("=== IntimacyAgent 호출 시작 ===");
        Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content)
            .doOnSubscribe(subscription -> log.info("IntimacyAgent 스트림 구독"))
            .doOnNext(resp -> {
                log.info("IntimacyAgent 완료: detectedLevel={}", resp.detectedLevel());
                sseManager.send(chatroomId, "intimacy_analysis", Map.of(
                    "detectedLevel", resp.detectedLevel(),
                    "correctedSentence", resp.correctedSentence(),
                    "feedback", Map.of("ko", resp.feedback().ko(), "en", resp.feedback().en()),
                    "corrections", resp.corrections(),
                    "alternativeExpressions", resp.alternativeExpressions() != null 
                        ? resp.alternativeExpressions().stream()
                            .map(alt -> Map.of(
                                "expression", alt.expression(),
                                "tone", alt.tone(),
                                "example", alt.example()
                            ))
                            .toList()
                        : List.of()
                ));
                updateIntimacyProgress(chatroomId, userId, resp);

                if (resp.correctedSentence() != null && "perfect".equalsIgnoreCase(resp.correctedSentence().trim())) {
                    userServiceClient.incrementPerfect(userId);
                }
            })
            .doOnError(ex -> log.error("IntimacyAgent 오류", ex))
            .doOnSuccess(resp -> log.info("IntimacyAgent 스트림 완료"))
            .cache(); // 결과 캐싱

        // 즉시 구독하여 SSE 전송 및 progress 업데이트 실행
        intimacyMono.subscribe();
        
        // Phase 2: Translation 제거됨 - VocabularyAgent가 모든 기능을 담당
        
        // Phase 3: Conversation (독립적 스트림)
        log.debug("=== ConversationAgent 호출 시작 ===");
        log.debug("ConversationAgent 파라미터 - chatroomId={}, content='{}'", chatroomId, content);
        // OpenAI usage(토큰 수)를 수집할 홀더
        java.util.concurrent.atomic.AtomicReference<OpenAIClient.Usage> usageHolder =
            new java.util.concurrent.atomic.AtomicReference<>(OpenAIClient.Usage.empty());
        
        conversationAgent.generateResponse(chatroomId, content, usageHolder)
            .doOnSubscribe(subscription -> log.debug("ConversationAgent 스트림 구독"))
            .doOnError(error -> log.error("ConversationAgent 스트림 오류", error))
            .collectList()
            .doOnSuccess(chunks -> {
                log.debug("collectList 성공, chunks: {}", chunks);
                String fullResponse = String.join("", chunks);
                log.debug("fullResponse: '{}'", fullResponse);
                
                // JSON 응답에서 실제 content만 추출
                String actualContent = extractContentFromJson(fullResponse);
                log.debug("extracted content: '{}'", actualContent);
                
                // OpenAI usage (입력/출력 토큰 수) 최종값
                OpenAIClient.Usage usage = usageHolder.get() != null
                    ? usageHolder.get()
                    : OpenAIClient.Usage.empty();
                
                // === 비동기로 Intimacy/Vocabulary 결과 수집 ===
                log.info("=== IntimacyAgent 결과 수집 시작 ===");
                intimacyMono
                    .doOnNext(intimacyResp -> {
                        log.info("IntimacyAgent 결과 수집 완료: detectedLevel={}, corrections='{}'", 
                            intimacyResp.detectedLevel(), intimacyResp.corrections());
                        
                        // VocabularyAgent 실행
                        log.debug("=== VocabularyAgent 호출 시작 (챗봇 응답 분석) ===");
                        log.debug("VocabularyAgent 파라미터 - botResponse='{}', chatroomId={}", actualContent, chatroomId);
                        
                        vocabularyAgent.extractDifficultWords(actualContent, chatroomId)
                            .doOnNext(vocabResp -> {
                                log.info("VocabularyAgent 결과 수집 완료: wordsCount={}", vocabResp.words().size());
                                
                                // SSE 전송
                                sseManager.send(chatroomId, "vocabulary_extracted", Map.of(
                                    "words", vocabResp.words().stream().map(w -> Map.of(
                                        "word", w.word(),
                                        "difficulty", w.difficulty(),
                                        "context", Map.of(
                                            "roma", w.context().roma(),
                                            "ko", w.context().ko(),
                                            "en", w.context().en()
                                        )
                                    )).toList()
                                ));
                                
                                // === Metadata 생성 및 봇 메시지 저장 ===
                                String metadataJson = null;
                                try {
                                    metadataJson = buildBotMetadata(userMessage.getId(), intimacyResp, vocabResp, usage);
                                    log.info("메타데이터 생성 성공: {}", metadataJson);
                                } catch (Exception e) {
                                    log.warn("메타데이터 생성 실패 - metadata 없이 저장합니다.", e);
                                }

                                if (shouldAbort(chatroomId, userMessage.getId(), userId)) {
                                    log.info("메시지 취소로 봇 응답 저장 중단: messageId={}", userMessage.getId());
                                    sseManager.send(chatroomId, "conversation_cancelled", Map.of("messageId", userMessage.getId()));
                                    return;
                                }

                                Message botMessage = chatService.sendMessage(
                                    chatroomId, null, "bot", actualContent, "text", metadataJson
                                );
                                
                                // SSE payload 구성 (usage가 비어있지 않으면 포함)
                                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                                payload.put("messageId", botMessage.getId());
                                payload.put("content", actualContent);
                                if (usage != null && !usage.isEmpty()) {
                                    java.util.Map<String, Object> usageMap = new java.util.HashMap<>();
                                    usageMap.put("inputTokens", usage.inputTokens());
                                    usageMap.put("outputTokens", usage.outputTokens());
                                    usageMap.put("total", usage.inputTokens() + usage.outputTokens());
                                    payload.put("usage", usageMap);
                                }
                                
                                sseManager.send(chatroomId, "conversation_complete", payload);
                                log.info("ConversationAgent 완료: messageId={}", botMessage.getId());
                                
                                // === SummarizerAgent 비동기 실행 (사용자 응답과 분리) ===
                                runSummarizerAsync(chatroomId, userId);
                            })
                            .doOnError(ex -> {
                                log.error("VocabularyAgent 오류", ex);
                                // VocabularyAgent 실패 시에도 봇 메시지 저장
                                String metadataJson = null;
                                try {
                                    metadataJson = buildBotMetadata(userMessage.getId(), intimacyResp, null, usage);
                                } catch (Exception e) {
                                    log.warn("메타데이터 생성 실패 - metadata 없이 저장합니다.", e);
                                }

                                if (shouldAbort(chatroomId, userMessage.getId(), userId)) {
                                    log.info("메시지 취소로 봇 응답 저장 중단: messageId={}", userMessage.getId());
                                    sseManager.send(chatroomId, "conversation_cancelled", Map.of("messageId", userMessage.getId()));
                                    return;
                                }

                                Message botMessage = chatService.sendMessage(
                                    chatroomId, null, "bot", actualContent, "text", metadataJson
                                );
                                
                                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                                payload.put("messageId", botMessage.getId());
                                payload.put("content", actualContent);
                                if (usage != null && !usage.isEmpty()) {
                                    java.util.Map<String, Object> usageMap = new java.util.HashMap<>();
                                    usageMap.put("inputTokens", usage.inputTokens());
                                    usageMap.put("outputTokens", usage.outputTokens());
                                    usageMap.put("total", usage.inputTokens() + usage.outputTokens());
                                    payload.put("usage", usageMap);
                                }
                                
                                sseManager.send(chatroomId, "conversation_complete", payload);
                                log.info("ConversationAgent 완료 (VocabularyAgent 실패): messageId={}", botMessage.getId());
                                
                                // === SummarizerAgent 비동기 실행 (사용자 응답과 분리) ===
                                runSummarizerAsync(chatroomId, userId);
                            })
                            .subscribe();
                    })
                    .doOnError(ex -> {
                        log.error("IntimacyAgent 결과 수집 실패", ex);
                        // IntimacyAgent 실패 시에도 봇 메시지 저장
                        if (shouldAbort(chatroomId, userMessage.getId(), userId)) {
                            log.info("메시지 취소로 봇 응답 저장 중단: messageId={}", userMessage.getId());
                            sseManager.send(chatroomId, "conversation_cancelled", Map.of("messageId", userMessage.getId()));
                            return;
                        }

                        Message botMessage = chatService.sendMessage(
                            chatroomId, null, "bot", actualContent, "text", null
                        );
                        
                        java.util.Map<String, Object> payload = new java.util.HashMap<>();
                        payload.put("messageId", botMessage.getId());
                        payload.put("content", actualContent);
                        // usage는 IntimacyAgent 오류와는 무관하므로, 이미 존재하면 그대로 사용
                        OpenAIClient.Usage usageLocal = usageHolder.get() != null
                            ? usageHolder.get()
                            : OpenAIClient.Usage.empty();
                        if (usageLocal != null && !usageLocal.isEmpty()) {
                            java.util.Map<String, Object> usageMap = new java.util.HashMap<>();
                            usageMap.put("inputTokens", usageLocal.inputTokens());
                            usageMap.put("outputTokens", usageLocal.outputTokens());
                            usageMap.put("total", usageLocal.inputTokens() + usageLocal.outputTokens());
                            payload.put("usage", usageMap);
                        }
                        
                        sseManager.send(chatroomId, "conversation_complete", payload);
                        log.info("ConversationAgent 완료 (IntimacyAgent 실패): messageId={}", botMessage.getId());
                        
                        // === SummarizerAgent 비동기 실행 (사용자 응답과 분리) ===
                        runSummarizerAsync(chatroomId, userId);
                    })
                    .subscribe();
            })
            .doOnError(ex -> {
                log.error("ConversationAgent 오류", ex);
                sseManager.send(chatroomId, "conversation_error", ex.getMessage());
            })
            .subscribe(
                result -> log.debug("ConversationAgent 구독 완료"),
                error -> log.error("ConversationAgent 구독 오류", error)
            );
        
        // Phase 4: IntimacyAgent 완료 대기 (VocabularyAgent는 챗봇 응답 후 별도 처리)
        log.debug("=== Phase 4: IntimacyAgent completion waiting ===");
        intimacyMono
            .doOnSubscribe(subscription -> log.debug("Phase 4 스트림 구독"))
            .doOnSuccess(intimacyResp -> {
                log.debug("Phase 4 성공 - intimacyResp 받음: {}", intimacyResp);
                log.info("Phase 4 완료 - IntimacyAgent 결과 처리됨");
            })
            .doOnError(ex -> {
                log.error("Multi-Agent 처리 중 오류", ex);
                sseManager.send(chatroomId, "agent_error", ex.getMessage());
            })
            .subscribe();
    }
    
    @Cacheable(value = "intimacy", key = "#chatroomId", unless = "#result == null")
    private int getCurrentIntimacyLevel(UUID chatroomId) {
        return intimacyProgressRepository.findByChatRoomId(chatroomId)
            .map(IntimacyProgress::getIntimacyLevel)
            .orElse(1);
    }
    
    /**
     * 친밀도 진척(totalCorrections, lastFeedback, progress_data)만 갱신한다.
     * 채팅방의 intimacyLevel은 여기서 갱신하지 않는다. start-greeting/createRoom/명시적 API에서만 설정되며,
     * 감지 결과(detectedLevel)는 이력(correctionsHistory)에만 기록하고 DB의 intimacy_level에는 반영하지 않는다.
     */
    private void updateIntimacyProgress(UUID chatroomId, UUID userId, IntimacyAgentResponse resp) {
        try {
            IntimacyProgress progress = intimacyProgressRepository.findByChatRoomId(chatroomId)
                .orElseGet(() -> {
                    // ChatRoom을 재조회하지 않고 프록시 사용
                    ChatRoom chatRoom = chatRoomRepository.getReferenceById(chatroomId);
                    return IntimacyProgress.builder()
                        .id(UUID.randomUUID())
                        .chatRoom(chatRoom)
                        .userId(userId)
                        .intimacyLevel(1)
                        .totalCorrections(0)
                        .build();
                });

            // corrections가 String으로 변경되어 빈 문자열이 아닌 경우에만 카운트 증가
            if (resp.corrections() != null && !resp.corrections().trim().isEmpty()) {
                progress.setTotalCorrections(progress.getTotalCorrections() + 1);
            }
            progress.setLastFeedback(resp.feedback().ko()); // FeedbackText에서 ko() 추출
            progress.setLastUpdated(LocalDateTime.now());

			// progress_data(JSONB)에 corrections 이력을 병합 저장
			try {
				ObjectMapper mapper = new ObjectMapper();
				String existing = progress.getProgressData();
				ObjectNode root = (existing != null && !existing.isBlank())
					? (ObjectNode) mapper.readTree(existing)
					: mapper.createObjectNode();
				ArrayNode history = root.withArray("correctionsHistory");
				ObjectNode entry = mapper.createObjectNode();
				entry.put("timestamp", LocalDateTime.now().toString());
				entry.put("detectedLevel", resp.detectedLevel());
				entry.put("correctedSentence", resp.correctedSentence());
				
				// FeedbackText를 JSON 객체로 저장
				ObjectNode feedbackNode = mapper.createObjectNode();
				feedbackNode.put("ko", resp.feedback().ko());
				feedbackNode.put("en", resp.feedback().en());
				entry.set("feedback", feedbackNode);
				
				// corrections를 단일 문자열로 저장
				entry.put("corrections", resp.corrections());
				history.add(entry);
				progress.setProgressData(mapper.writeValueAsString(root));
			} catch (Exception jsonEx) {
				log.warn("progress_data 병합 실패 - 무시하고 진행합니다.", jsonEx);
			}
            
            intimacyProgressRepository.save(progress);
            log.debug("친밀도 진척 업데이트: chatroomId={}, storedLevel={}, detectedLevel={}, corrections={}",
                chatroomId, progress.getIntimacyLevel(), resp.detectedLevel(), progress.getTotalCorrections());
        } catch (Exception e) {
            log.error("친밀도 진척 업데이트 실패: chatroomId={}", chatroomId, e);
        }
    }
    
    /**
     * JSON 응답에서 실제 content만 추출
     */
    private String extractContentFromJson(String jsonResponse) {
        try {
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                return "";
            }
            
            // JSON 파싱 시도
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(jsonResponse);
            
            if (root.has("content")) {
                String content = root.get("content").asText();
                log.debug("JSON에서 content 추출 성공: '{}'", content);
                return content;
            } else {
                log.warn("JSON에 content 필드가 없음: {}", jsonResponse);
                return jsonResponse; // JSON이 아니면 원본 반환
            }
        } catch (Exception e) {
            log.debug("JSON 파싱 실패, 원본 텍스트 반환: {}", e.getMessage());
            return jsonResponse; // JSON 파싱 실패시 원본 반환
        }
    }

    /**
     * Bot 메시지에 저장할 metadata JSON 생성
     */
    private String buildBotMetadata(UUID userMessageId,
                                    IntimacyAgentResponse intimacyResp,
                                    VocabularyAgentResponse vocabResp,
                                    OpenAIClient.Usage usage) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        // userMessageAnalysis
        ObjectNode userAnalysis = mapper.createObjectNode();
        userAnalysis.put("userMessageId", userMessageId != null ? userMessageId.toString() : null);
        // intimacy: 항상 기본 구조 포함 (파싱 일관성 보장)
        ObjectNode intimacy = mapper.createObjectNode();
        if (intimacyResp != null) {
            intimacy.put("detectedLevel", intimacyResp.detectedLevel());
            intimacy.put("correctedSentence", intimacyResp.correctedSentence());
            ObjectNode feedback = mapper.createObjectNode();
            feedback.put("ko", intimacyResp.feedback().ko());
            feedback.put("en", intimacyResp.feedback().en());
            intimacy.set("feedback", feedback);
            intimacy.put("corrections", intimacyResp.corrections());
            // alternativeExpressions 추가
            ArrayNode alternatives = mapper.createArrayNode();
            if (intimacyResp.alternativeExpressions() != null) {
                for (var alt : intimacyResp.alternativeExpressions()) {
                    ObjectNode altNode = mapper.createObjectNode();
                    altNode.put("expression", alt.expression());
                    altNode.put("tone", alt.tone());
                    altNode.put("example", alt.example());
                    alternatives.add(altNode);
                }
            }
            intimacy.set("alternativeExpressions", alternatives);
        } else {
            // 기본값
            intimacy.put("detectedLevel", 0);
            intimacy.put("correctedSentence", "");
            ObjectNode feedback = mapper.createObjectNode();
            feedback.put("ko", "");
            feedback.put("en", "");
            intimacy.set("feedback", feedback);
            intimacy.put("corrections", "");
            intimacy.set("alternativeExpressions", mapper.createArrayNode());
        }
        userAnalysis.set("intimacy", intimacy);
        root.set("userMessageAnalysis", userAnalysis);

        // botResponseAnalysis
        ObjectNode botAnalysis = mapper.createObjectNode();
        if (vocabResp != null && vocabResp.words() != null && !vocabResp.words().isEmpty()) {
            ObjectNode vocabulary = mapper.createObjectNode();
            ArrayNode words = mapper.createArrayNode();
            for (var w : vocabResp.words()) {
                ObjectNode word = mapper.createObjectNode();
                word.put("word", w.word());
                word.put("difficulty", w.difficulty());
                ObjectNode context = mapper.createObjectNode();
                context.put("roma", w.context().roma());
                context.put("ko", w.context().ko());
                context.put("en", w.context().en());
                word.set("context", context);
                words.add(word);
            }
            vocabulary.set("words", words);
            botAnalysis.set("vocabulary", vocabulary);
        }
        root.set("botResponseAnalysis", botAnalysis);
        
        // usage 정보 추가 (있을 경우에만)
        if (usage != null && !usage.isEmpty()) {
            ObjectNode usageNode = mapper.createObjectNode();
            usageNode.put("inputTokens", usage.inputTokens());
            usageNode.put("outputTokens", usage.outputTokens());
            usageNode.put("total", usage.inputTokens() + usage.outputTokens());
            root.set("usage", usageNode);
        }
        
        return mapper.writeValueAsString(root);
    }
    
    /**
     * SummarizerAgent를 완전 비동기로 실행 (사용자 응답과 분리)
     * conversation_complete 전송 후 백그라운드에서 실행되어 사용자 응답 시간에 영향 없음
     */
    private void runSummarizerAsync(UUID chatroomId, UUID userId) {
        Mono.fromCallable(() -> {
            // 이전 요약(compact) 추출
            IntimacyProgress progress = intimacyProgressRepository.findByChatRoomId(chatroomId).orElse(null);
            ObjectMapper mapper = new ObjectMapper();
            String previousSummaryCompact = null;
            
            if (progress != null && progress.getProgressData() != null && !progress.getProgressData().isBlank()) {
                try {
                    ObjectNode rootPrev = (ObjectNode) mapper.readTree(progress.getProgressData());
                    if (rootPrev.has("summaryHistory") && rootPrev.get("summaryHistory").isArray() && rootPrev.get("summaryHistory").size() > 0) {
                        JsonNode last = rootPrev.get("summaryHistory").get(rootPrev.get("summaryHistory").size() - 1);
                        if (last.has("summary")) {
                            previousSummaryCompact = last.get("summary").toString();
                        }
                    }
                } catch (Exception e) {
                    log.warn("이전 요약 추출 실패: {}", e.getMessage());
                }
            }
            
            // SummarizerAgent 실행 (최근 K=20)
            return summarizerAgent.summarize(chatroomId, 20, previousSummaryCompact);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(sr -> {
            try {
                // progress_data 병합
                IntimacyProgress progress = intimacyProgressRepository.findByChatRoomId(chatroomId).orElse(null);
                ObjectMapper mapper = new ObjectMapper();
                
                ObjectNode root = (progress != null && progress.getProgressData() != null && !progress.getProgressData().isBlank())
                    ? (ObjectNode) mapper.readTree(progress.getProgressData())
                    : mapper.createObjectNode();

                // summaryHistory append
                ArrayNode sh = root.withArray("summaryHistory");
                ObjectNode entry = mapper.createObjectNode();
                entry.put("id", UUID.randomUUID().toString());
                entry.put("timestamp", sr.timestamp);
                ObjectNode range = mapper.createObjectNode();
                range.put("startSeq", sr.windowStartSeq);
                range.put("endSeq", sr.windowEndSeq);
                entry.set("range", range);
                JsonNode summaryNode = mapper.readTree(sr.summary == null || sr.summary.isBlank() ? "{}" : sr.summary);
                entry.set("summary", summaryNode);
                sh.add(entry);

                // keywordIndex upsert
                ObjectNode ki = (ObjectNode) root.with("keywordIndex");
                ArrayNode items = ki.withArray("items");
                for (String kw : sr.keywords) {
                    boolean merged = false;
                    for (int i = 0; i < items.size(); i++) {
                        ObjectNode it = (ObjectNode) items.get(i);
                        if (kw.equalsIgnoreCase(it.path("keyword").asText())) {
                            it.put("score", it.path("score").asInt(0) + 1);
                            it.put("updatedAt", java.time.OffsetDateTime.now().toString());
                            merged = true;
                            break;
                        }
                    }
                    if (!merged) {
                        ObjectNode it = mapper.createObjectNode();
                        it.put("keyword", kw);
                        it.put("score", 1);
                        it.put("updatedAt", java.time.OffsetDateTime.now().toString());
                        it.set("occurrences", mapper.createArrayNode());
                        items.add(it);
                    }
                }
                
                // 상한치 적용: 키워드 50개 제한, summaryHistory 2개 제한
                if (items.size() > 50) {
                    // 점수 기준으로 정렬 후 상위 50개만 유지
                    List<ObjectNode> sortedItems = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        sortedItems.add((ObjectNode) items.get(i));
                    }
                    sortedItems.sort((a, b) -> Integer.compare(b.path("score").asInt(0), a.path("score").asInt(0)));
                    
                    ArrayNode newItems = mapper.createArrayNode();
                    for (int i = 0; i < Math.min(50, sortedItems.size()); i++) {
                        newItems.add(sortedItems.get(i));
                    }
                    ki.set("items", newItems);
                }
                
                // summaryHistory 2개 제한
                ArrayNode summaryHistory = root.withArray("summaryHistory");
                if (summaryHistory.size() > 2) {
                    ArrayNode newSh = mapper.createArrayNode();
                    for (int i = summaryHistory.size() - 2; i < summaryHistory.size(); i++) {
                        newSh.add(summaryHistory.get(i));
                    }
                    root.set("summaryHistory", newSh);
                }

                // lastContextSnapshot 갱신
                ObjectNode snap = (ObjectNode) root.with("lastContextSnapshot");
                snap.put("usedAt", java.time.OffsetDateTime.now().toString());
                snap.put("intimacyLevel", getCurrentIntimacyLevel(chatroomId));
                ObjectNode mw = mapper.createObjectNode();
                mw.put("startSeq", sr.windowStartSeq);
                mw.put("endSeq", sr.windowEndSeq);
                mw.put("count", (sr.windowEndSeq - sr.windowStartSeq + 1));
                snap.set("messagesWindow", mw);
                ArrayNode sid = mapper.createArrayNode();
                sid.add(entry.get("id").asText());
                snap.set("summaryIds", sid);
                snap.set("keywordHashes", mapper.createArrayNode());

                if (progress == null) {
                    ChatRoom chatRoom = chatService.getChatRoomById(chatroomId);
                    progress = IntimacyProgress.builder()
                        .id(UUID.randomUUID())
                        .chatRoom(chatRoom)
                        .userId(userId)
                        .intimacyLevel(getCurrentIntimacyLevel(chatroomId))
                        .totalCorrections(0)
                        .build();
                }
                progress.setProgressData(mapper.writeValueAsString(root));
                progress.setLastUpdated(LocalDateTime.now());
                intimacyProgressRepository.save(progress);
                log.info("요약/키워드 저장 완료: chatroomId={}, summaryHistorySize={}, keywordCount={}, tokens={}", 
                    chatroomId, summaryHistory.size(), items.size(), sr.tokens);
            } catch (Exception ex) {
                log.warn("요약/키워드 후처리 실패 - 무시하고 진행합니다.", ex);
            }
        })
        .doOnError(ex -> {
            log.warn("요약/키워드 후처리 실패 - 무시하고 진행합니다.", ex);
        })
        .subscribe(); // 비동기 실행, 결과 대기하지 않음
    }

    private boolean shouldAbort(UUID chatroomId, UUID messageId, UUID userId) {
        if (chatService.isMessageCancelled(messageId)) {
            return true;
        }
		if (!sseManager.hasAnySubscribers(chatroomId)) {
            try {
                chatService.cancelMessage(messageId, userId);
            } catch (Exception ignored) {}
            return true;
        }
        return false;
    }
}
