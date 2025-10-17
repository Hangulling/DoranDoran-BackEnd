package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.sse.SSEManager;
import com.dorandoran.chat.service.agent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    
    public void processUserMessage(UUID chatroomId, UUID userId, Message userMessage) {
        log.info("=== MultiAgentOrchestrator.processUserMessage() 호출됨 ===");
        String content = userMessage.getContent();
        int userLevel = getCurrentIntimacyLevel(chatroomId);
        
        log.info("Multi-Agent 처리 시작: chatroomId={}, userId={}, userLevel={}", chatroomId, userId, userLevel);
        
        // Phase 1: 병렬 실행 (Intimacy, Vocabulary, Conversation)
        log.debug("=== Phase 1: Parallel execution started ===");
        log.debug("=== IntimacyAgent 호출 시작 ===");
        Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content)
            .doOnSubscribe(subscription -> log.debug("IntimacyAgent 스트림 구독"))
            .doOnNext(resp -> {
                log.debug("IntimacyAgent 완료: detectedLevel={}", resp.detectedLevel());
                sseManager.send(chatroomId, "intimacy_analysis", Map.of(
                    "detectedLevel", resp.detectedLevel(),
                    "correctedSentence", resp.correctedSentence(),
                    "feedback", resp.feedback(),
                    "corrections", resp.corrections()
                ));
                updateIntimacyProgress(chatroomId, userId, resp);
            })
            .doOnError(ex -> log.error("IntimacyAgent 오류", ex))
            .doOnSuccess(resp -> log.debug("IntimacyAgent 스트림 완료"))
            .cache(); // 결과 캐싱

        // 즉시 구독
        intimacyMono.subscribe();
        
        log.debug("=== VocabularyAgent 호출 시작 ===");
        log.debug("VocabularyAgent 파라미터 - content='{}', userLevel={}", content, userLevel);
        Mono<VocabularyAgentResponse> vocabularyMono = vocabularyAgent.extractDifficultWords(content, userLevel)
            .doOnSubscribe(subscription -> log.debug("VocabularyAgent 스트림 구독"))
            .doOnNext(resp -> {
                log.debug("VocabularyAgent 완료: wordsCount={}", resp.words().size());
                sseManager.send(chatroomId, "vocabulary_extracted", Map.of(
                    "words", resp.words().stream().map(w -> Map.of(
                        "word", w.word(),
                        "difficulty", w.difficulty(),
                        "context", Map.of(
                            "roma", w.context().roma(),
                            "ko", w.context().ko(),
                            "en", w.context().en()
                        )
                    )).toList()
                ));
            })
            .doOnError(ex -> log.error("VocabularyAgent 오류", ex))
            .doOnSuccess(resp -> log.debug("VocabularyAgent 스트림 완료"))
            .cache(); // 결과 캐싱

        // 즉시 구독
        vocabularyMono.subscribe();
        
        // Phase 2: Translation 제거됨 - VocabularyAgent가 모든 기능을 담당
        
        // Phase 3: Conversation (독립적 스트림)
        log.debug("=== ConversationAgent 호출 시작 ===");
        log.debug("ConversationAgent 파라미터 - chatroomId={}, content='{}'", chatroomId, content);
        conversationAgent.generateResponse(chatroomId, content)
            .doOnSubscribe(subscription -> log.debug("ConversationAgent 스트림 구독"))
            .doOnNext(chunk -> sseManager.send(chatroomId, "conversation_chunk", chunk))
            .doOnError(error -> log.error("ConversationAgent 스트림 오류", error))
            .collectList()
            .doOnSuccess(chunks -> {
                log.debug("collectList 성공, chunks: {}", chunks);
                String fullResponse = String.join("", chunks);
                log.debug("fullResponse: '{}'", fullResponse);
                Message botMessage = chatService.sendMessage(
                    chatroomId, null, "bot", fullResponse, "text"
                );
                sseManager.send(chatroomId, "conversation_complete", Map.of(
                    "messageId", botMessage.getId(),
                    "content", fullResponse
                ));
                log.info("ConversationAgent 완료: messageId={}", botMessage.getId());

                // === 후처리: 요약/키워드 생성 및 progress_data 병합 저장 ===
                try {
                    // 이전 요약(compact) 추출
                    String previousSummaryCompact = null;
                    IntimacyProgress progress = intimacyProgressRepository.findByChatRoomId(chatroomId).orElse(null);
                    ObjectMapper mapper = new ObjectMapper();
                    if (progress != null && progress.getProgressData() != null && !progress.getProgressData().isBlank()) {
                        ObjectNode rootPrev = (ObjectNode) mapper.readTree(progress.getProgressData());
                        if (rootPrev.has("summaryHistory") && rootPrev.get("summaryHistory").isArray() && rootPrev.get("summaryHistory").size() > 0) {
                            JsonNode last = rootPrev.get("summaryHistory").get(rootPrev.get("summaryHistory").size() - 1);
                            if (last.has("summary")) {
                                previousSummaryCompact = last.get("summary").toString();
                            }
                        }
                    }

                    // Summarizer 실행 (최근 K=20)
                    SummarizerAgent.SummaryResult sr = summarizerAgent.summarize(chatroomId, 20, previousSummaryCompact);

                    // progress_data 병합
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
                log.error("ConversationAgent 오류", ex);
                sseManager.send(chatroomId, "conversation_error", ex.getMessage());
            })
            .subscribe(
                result -> log.debug("ConversationAgent 구독 완료"),
                error -> log.error("ConversationAgent 구독 오류", error)
            );
        
        // Phase 4: 병렬 실행 완료 대기 및 통합 결과 전송
        log.debug("=== Phase 4: Parallel execution completion waiting ===");
        Mono.zip(intimacyMono, vocabularyMono)
            .doOnSubscribe(subscription -> log.debug("Phase 4 스트림 구독"))
            .doOnSuccess(tuple -> {
                log.debug("Phase 4 성공 - tuple 받음");
                IntimacyAgentResponse intimacyResp = tuple.getT1();
                VocabularyAgentResponse vocabResp = tuple.getT2();
                
                Map<String, Object> aggregatedResult = new HashMap<>();
                aggregatedResult.put("intimacy", Map.of(
                    "detectedLevel", intimacyResp.detectedLevel(),
                    "correctedSentence", intimacyResp.correctedSentence(),
                    "feedback", intimacyResp.feedback()
                ));
                aggregatedResult.put("vocabulary", Map.of(
                    "words", vocabResp.words().size()
                ));
                
                sseManager.send(chatroomId, "aggregated_complete", aggregatedResult);
                log.info("Multi-Agent 처리 완료: chatroomId={}, intimacyLevel={}, vocabCount={}", 
                    chatroomId, intimacyResp.detectedLevel(), vocabResp.words().size());
            })
            .doOnError(ex -> {
                log.error("Multi-Agent 처리 중 오류", ex);
                sseManager.send(chatroomId, "agent_error", ex.getMessage());
            })
            .subscribe();
    }
    
    private int getCurrentIntimacyLevel(UUID chatroomId) {
        return intimacyProgressRepository.findByChatRoomId(chatroomId)
            .map(IntimacyProgress::getIntimacyLevel)
            .orElse(1);
    }
    
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
            
            progress.setIntimacyLevel(resp.detectedLevel());
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
            log.debug("친밀도 진척 업데이트: chatroomId={}, level={}, corrections={}", 
                chatroomId, resp.detectedLevel(), progress.getTotalCorrections());
        } catch (Exception e) {
            log.error("친밀도 진척 업데이트 실패: chatroomId={}", chatroomId, e);
        }
    }
}
