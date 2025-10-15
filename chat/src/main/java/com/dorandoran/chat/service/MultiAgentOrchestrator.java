package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.dorandoran.chat.messaging.RedisMessagePublisher;
import com.dorandoran.chat.service.agent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    private final TranslationAgent translationAgent;
    private final ConversationAgent conversationAgent;
    private final SSEManager sseManager;
    private final IntimacyProgressRepository intimacyProgressRepository;
    private final ChatService chatService;
    
    public void processUserMessage(UUID chatroomId, UUID userId, Message userMessage) {
        log.info("=== MultiAgentOrchestrator.processUserMessage() 호출됨 ===");
        String content = userMessage.getContent();
        int userLevel = getCurrentIntimacyLevel(chatroomId);
        
        log.info("Multi-Agent 처리 시작: chatroomId={}, userId={}, userLevel={}", chatroomId, userId, userLevel);
        
        // Phase 1: 병렬 실행 (Intimacy, Vocabulary, Conversation)
        log.info("=== MultiAgentOrchestrator: Phase 1 시작 ===");
        log.info("=== MultiAgentOrchestrator: IntimacyAgent 호출 시작 ===");
        Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content)
            .doOnSubscribe(subscription -> log.info("=== MultiAgentOrchestrator: IntimacyAgent 스트림 구독 ==="))
            .doOnNext(resp -> {
                log.info("=== IntimacyAgent 완료: {} ===", resp);
                sseManager.send(chatroomId, "intimacy_analysis", Map.of(
                    "detectedLevel", resp.detectedLevel(),
                    "correctedSentence", resp.correctedSentence(),
                    "feedback", resp.feedback(),
                    "corrections", resp.corrections()
                ));
                updateIntimacyProgress(chatroomId, userId, resp);
            })
            .doOnError(ex -> log.error("=== IntimacyAgent 오류 ===", ex))
            .doOnSuccess(resp -> log.info("=== MultiAgentOrchestrator: IntimacyAgent 스트림 완료 ==="));
        
        log.info("=== MultiAgentOrchestrator: VocabularyAgent 호출 시작 ===");
        log.info("=== MultiAgentOrchestrator: VocabularyAgent 파라미터 - content='{}', userLevel={} ===", content, userLevel);
        Mono<VocabularyAgentResponse> vocabularyMono = vocabularyAgent.extractDifficultWords(content, userLevel)
            .cache()  // ← 추가: 결과를 캐시하여 여러 번 구독 가능
            .doOnSubscribe(subscription -> log.info("=== MultiAgentOrchestrator: VocabularyAgent 스트림 구독 ==="))
            .doOnNext(resp -> {
                log.info("=== VocabularyAgent 완료: {} ===", resp);
                sseManager.send(chatroomId, "vocabulary_extracted", Map.of(
                    "words", resp.words().stream().map(w -> Map.of(
                        "word", w.word(),
                        "difficulty", w.difficulty(),
                        "context", w.context()
                    )).toList()
                ));
            })
            .doOnError(ex -> log.error("=== VocabularyAgent 오류 ===", ex))
            .doOnSuccess(resp -> log.info("=== MultiAgentOrchestrator: VocabularyAgent 스트림 완료 ==="));
        
        // Phase 2: Translation (Vocabulary 의존)
        log.info("=== MultiAgentOrchestrator: TranslationAgent 호출 시작 ===");
        vocabularyMono
            .flatMap(vocabResp -> {
                log.info("=== MultiAgentOrchestrator: TranslationAgent - vocabResp 받음: {} ===", vocabResp);
                return translationAgent.translate(vocabResp.words());
            })
            .doOnSubscribe(subscription -> log.info("=== MultiAgentOrchestrator: TranslationAgent 스트림 구독 ==="))
            .doOnNext(resp -> {
                log.info("=== TranslationAgent 완료: {} ===", resp);
                sseManager.send(chatroomId, "vocabulary_translated", Map.of(
                    "translations", resp.translations().stream().map(t -> Map.of(
                        "original", t.original(),
                        "english", t.english(),
                        "pronunciation", t.pronunciation()
                    )).toList()
                ));
            })
            .doOnError(ex -> log.error("=== TranslationAgent 오류 ===", ex))
            .doOnSuccess(resp -> log.info("=== MultiAgentOrchestrator: TranslationAgent 스트림 완료 ==="))
            .subscribe();
        
        // Phase 3: Conversation (독립적 스트림)
        log.info("=== MultiAgentOrchestrator: ConversationAgent 호출 시작 ===");
        log.info("=== MultiAgentOrchestrator: ConversationAgent 파라미터 - chatroomId={}, content='{}' ===", chatroomId, content);
        conversationAgent.generateResponse(chatroomId, content)
            .doOnSubscribe(subscription -> log.info("=== MultiAgentOrchestrator: ConversationAgent 스트림 구독 ==="))
            .doOnNext(chunk -> {
                log.info("=== MultiAgentOrchestrator: conversation_chunk 받음: '{}' ===", chunk);
                sseManager.send(chatroomId, "conversation_chunk", chunk);
            })
            .doOnError(error -> log.error("=== MultiAgentOrchestrator: ConversationAgent 스트림 오류 ===", error))
            .collectList()
            .doOnSuccess(chunks -> {
                log.info("=== MultiAgentOrchestrator: collectList 성공, chunks: {} ===", chunks);
                String fullResponse = String.join("", chunks);
                log.info("=== MultiAgentOrchestrator: fullResponse: '{}' ===", fullResponse);
                Message botMessage = chatService.sendMessage(
                    chatroomId, null, "bot", fullResponse, "text"
                );
                sseManager.send(chatroomId, "conversation_complete", Map.of(
                    "messageId", botMessage.getId(),
                    "content", fullResponse
                ));
                log.info("ConversationAgent 완료: messageId={}", botMessage.getId());
            })
            .doOnError(ex -> {
                log.error("=== MultiAgentOrchestrator: ConversationAgent 오류 ===", ex);
                sseManager.send(chatroomId, "conversation_error", ex.getMessage());
            })
            .subscribe(
                result -> log.info("=== MultiAgentOrchestrator: ConversationAgent 구독 완료 ==="),
                error -> log.error("=== MultiAgentOrchestrator: ConversationAgent 구독 오류 ===", error)
            );
        
        // Phase 4: 병렬 실행 완료 대기 및 통합 결과 전송
        log.info("=== MultiAgentOrchestrator: Phase 4 시작 - 병렬 실행 완료 대기 ===");
        Mono.zip(intimacyMono, vocabularyMono)
            .doOnSubscribe(subscription -> log.info("=== MultiAgentOrchestrator: Phase 4 스트림 구독 ==="))
            .doOnSuccess(tuple -> {
                log.info("=== MultiAgentOrchestrator: Phase 4 성공 - tuple 받음 ===");
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
                log.info("Multi-Agent 처리 완료: chatroomId={}", chatroomId);
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
                    // 새로 생성할 때만 ChatRoom 객체 설정
                    ChatRoom chatRoom = chatService.getChatRoomById(chatroomId);
                    return IntimacyProgress.builder()
                        .id(UUID.randomUUID())
                        .chatRoom(chatRoom)
                        .userId(userId)
                        .intimacyLevel(1)
                        .totalCorrections(0)
                        .build();
                });
            
            progress.setIntimacyLevel(resp.detectedLevel());
            progress.setTotalCorrections(progress.getTotalCorrections() + resp.corrections().size());
            progress.setLastFeedback(resp.feedback());
            progress.setLastUpdated(LocalDateTime.now());
            
            intimacyProgressRepository.save(progress);
            log.debug("친밀도 진척 업데이트: chatroomId={}, level={}, corrections={}", 
                chatroomId, resp.detectedLevel(), progress.getTotalCorrections());
        } catch (Exception e) {
            log.error("친밀도 진척 업데이트 실패: chatroomId={}", chatroomId, e);
        }
    }
  private final IntimacyAgent intimacyAgent;
  private final VocabularyAgent vocabularyAgent;
  private final TranslationAgent translationAgent;
  private final ConversationAgent conversationAgent;
  private final IntimacyProgressRepository intimacyProgressRepository;
  private final ChatService chatService;
  private final RedisMessagePublisher redisPublisher;

  public void processUserMessage(UUID chatroomId, UUID userId, Message userMessage) {
    log.info("=== MultiAgentOrchestrator 시작: chatroomId={}, userId={} ===", chatroomId, userId);
    String content = userMessage.getContent();
    int userLevel = getCurrentIntimacyLevel(chatroomId);

    // Phase 1: 병렬 실행 (Intimacy, Vocabulary, Conversation)
    Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content)
        .doOnNext(resp -> {
          log.info("IntimacyAgent 완료: level={}", resp.detectedLevel());
          // Redis Pub/Sub으로 발행
          redisPublisher.publishSSEEvent(chatroomId, "intimacy_analysis", Map.of(
              "detectedLevel", resp.detectedLevel(),
              "correctedSentence", resp.correctedSentence(),
              "feedback", resp.feedback(),
              "corrections", resp.corrections()
          ));
          updateIntimacyProgress(chatroomId, userId, resp);
        })
        .doOnError(ex -> log.error("IntimacyAgent 오류", ex));

    Mono<VocabularyAgentResponse> vocabularyMono = vocabularyAgent.extractDifficultWords(content, userLevel)
        .doOnNext(resp -> {
          log.info("VocabularyAgent 완료: words={}", resp.words().size());
          // Redis Pub/Sub으로 발행
          redisPublisher.publishSSEEvent(chatroomId, "vocabulary_extracted", Map.of(
              "words", resp.words().stream().map(w -> Map.of(
                  "word", w.word(),
                  "difficulty", w.difficulty(),
                  "context", w.context()
              )).toList()
          ));
        })
        .doOnError(ex -> log.error("VocabularyAgent 오류", ex));

    // Phase 2: Translation (Vocabulary 의존)
    vocabularyMono
        .flatMap(vocabResp -> translationAgent.translate(vocabResp.words()))
        .doOnNext(resp -> {
          log.info("TranslationAgent 완료: translations={}", resp.translations().size());
          // Redis Pub/Sub으로 발행
          redisPublisher.publishSSEEvent(chatroomId, "vocabulary_translated", Map.of(
              "translations", resp.translations().stream().map(t -> Map.of(
                  "original", t.original(),
                  "english", t.english(),
                  "pronunciation", t.pronunciation()
              )).toList()
          ));
        })
        .doOnError(ex -> log.error("TranslationAgent 오류", ex))
        .subscribe();

    // Phase 3: Conversation (독립적 스트림)
    conversationAgent.generateResponse(chatroomId, content)
        .doOnNext(chunk -> {
          log.debug("conversation_chunk: '{}'", chunk);
          // Redis Pub/Sub으로 발행
          redisPublisher.publishSSEEvent(chatroomId, "conversation_chunk", chunk);
        })
        .collectList()
        .doOnSuccess(chunks -> {
          String fullResponse = String.join("", chunks);
          log.info("ConversationAgent 완료: length={}", fullResponse.length());

          Message botMessage = chatService.sendMessage(
              chatroomId, null, "bot", fullResponse, "text"
          );

          // Redis Pub/Sub으로 발행
          redisPublisher.publishSSEEvent(chatroomId, "conversation_complete", Map.of(
              "messageId", botMessage.getId(),
              "content", fullResponse
          ));
        })
        .doOnError(ex -> {
          log.error("ConversationAgent 오류", ex);
          redisPublisher.publishSSEEvent(chatroomId, "conversation_error", ex.getMessage());
        })
        .subscribe();

    // Phase 4: 병렬 실행 완료 대기 및 통합 결과 전송
    Mono.zip(intimacyMono, vocabularyMono)
        .doOnSuccess(tuple -> {
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

          // Redis Pub/Sub으로 발행
          redisPublisher.publishSSEEvent(chatroomId, "aggregated_complete", aggregatedResult);
          log.info("Multi-Agent 처리 완료: chatroomId={}", chatroomId);
        })
        .doOnError(ex -> {
          log.error("Multi-Agent 처리 중 오류", ex);
          redisPublisher.publishSSEEvent(chatroomId, "agent_error", ex.getMessage());
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
            ChatRoom chatRoom = chatService.getChatRoomById(chatroomId);
            return IntimacyProgress.builder()
                .id(UUID.randomUUID())
                .chatRoom(chatRoom)
                .userId(userId)
                .intimacyLevel(1)
                .totalCorrections(0)
                .build();
          });

      progress.setIntimacyLevel(resp.detectedLevel());
      progress.setTotalCorrections(progress.getTotalCorrections() + resp.corrections().size());
      progress.setLastFeedback(resp.feedback());
      progress.setLastUpdated(LocalDateTime.now());

      intimacyProgressRepository.save(progress);
      log.debug("친밀도 진척 업데이트: chatroomId={}, level={}", chatroomId, resp.detectedLevel());
    } catch (Exception e) {
      log.error("친밀도 진척 업데이트 실패: chatroomId={}", chatroomId, e);
    }
  }
}