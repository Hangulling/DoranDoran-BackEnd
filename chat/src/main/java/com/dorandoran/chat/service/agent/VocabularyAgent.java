package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * 어휘 추출 Agent (Facade)
 * 내부적으로 VocabularyExtractionAgent와 VocabularyExplanationAgent를 조합하여 사용
 * 하위 호환성을 위해 기존 인터페이스 유지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyAgent {
    private final VocabularyExtractionAgent extractionAgent;
    private final VocabularyExplanationAgent explanationAgent;
    private final ChatService chatService;

    /**
     * 챗봇 응답에서 어려운 단어/표현을 추출 (기존 인터페이스 유지)
     * 
     * @param botResponse 챗봇 응답 문장
     * @return 어휘 추출 결과
     */
    public Mono<VocabularyAgentResponse> extractDifficultWords(String botResponse) {
        log.info("=== VocabularyAgent.extractDifficultWords() 호출됨 (Facade) ===");
        log.info("=== VocabularyAgent 파라미터 - botResponse='{}' ===", botResponse);
        
        // 컨셉과 레벨 정보가 없으면 기본값 사용
        // 실제로는 MultiAgentOrchestrator에서 chatroomId를 전달받아야 하지만,
        // 하위 호환성을 위해 기본값으로 처리
        String concept = "FRIEND";
        int intimacyLevel = 2;
        
        log.warn("VocabularyAgent: chatroomId 없이 호출됨 - 기본값 사용 (concept={}, intimacyLevel={})", 
            concept, intimacyLevel);
        
        return extractDifficultWords(botResponse, concept, intimacyLevel);
    }
    
    /**
     * 챗봇 응답에서 어려운 단어/표현을 추출 (컨셉/레벨 포함)
     * 
     * @param botResponse 챗봇 응답 문장
     * @param concept 컨셉 (FRIEND, COWORKER, BOSS, SENIOR, HONEY)
     * @param intimacyLevel 친밀도 레벨 (1-3)
     * @return 어휘 추출 결과
     */
    public Mono<VocabularyAgentResponse> extractDifficultWords(String botResponse, String concept, int intimacyLevel) {
        log.info("=== VocabularyAgent.extractDifficultWords() 호출됨 (Facade) ===");
        log.info("=== 파라미터 - botResponse='{}', concept='{}', intimacyLevel={} ===", 
            botResponse, concept, intimacyLevel);
        
        // 1. ExtractionAgent로 어휘 추출
        return extractionAgent.extract(botResponse, concept, intimacyLevel)
            .doOnNext(extractionResult -> {
                if (extractionResult != null) {
                    log.info("=== VocabularyAgent: ExtractionAgent 응답 수신 ===");
                    log.info("  - originalExpression: '{}'", extractionResult.originalExpression());
                    log.info("  - rootForm: '{}'", extractionResult.rootForm());
                    log.info("  - category: {}", extractionResult.category());
                    log.info("  - difficulty: {}", extractionResult.difficulty());
                    log.info("  - reason: '{}'", extractionResult.reason());
                } else {
                    log.info("=== VocabularyAgent: ExtractionAgent 응답 - 추출된 어휘 없음 ===");
                }
            })
            .flatMap(extractionResult -> {
                // 추출 결과가 null이면 빈 응답 반환
                if (extractionResult == null) {
                    log.info("VocabularyAgent: 추출된 어휘 없음 - 빈 응답 반환");
                    return Mono.just(new VocabularyAgentResponse("vocabulary", List.of()));
                }
                
                // 2. ExplanationAgent로 설명 생성
                return explanationAgent.generateExplanation(extractionResult, concept, intimacyLevel)
                    .doOnNext(explanationResult -> {
                        log.info("=== VocabularyAgent: ExplanationAgent 응답 수신 ===");
                        log.info("  - roma: '{}'", explanationResult.roma());
                        log.info("  - ko: '{}'", explanationResult.ko());
                        log.info("  - en: '{}'", explanationResult.en());
                    })
                    .map(explanationResult -> {
                        // 3. 최종 응답 형식으로 변환
                        VocabularyAgentResponse.VocabularyWord word = new VocabularyAgentResponse.VocabularyWord(
                            extractionResult.rootForm(),
                            extractionResult.difficulty(),
                            new VocabularyAgentResponse.Context(
                                explanationResult.roma(),
                                explanationResult.ko(),
                                explanationResult.en()
                            )
                        );
                        
                        log.info("=== VocabularyAgent 최종 응답 ===");
                        log.info("  - word: '{}'", word.word());
                        log.info("  - difficulty: {}", word.difficulty());
                        log.info("  - context.roma: '{}'", word.context().roma());
                        log.info("  - context.ko: '{}'", word.context().ko());
                        log.info("  - context.en: '{}'", word.context().en());
                        log.info("=== VocabularyAgent 최종 응답 완료 ===");
                        
                        return new VocabularyAgentResponse("vocabulary", List.of(word));
                    });
            })
            .onErrorResume(error -> {
                log.error("VocabularyAgent 처리 오류", error);
                // 오류 발생 시 빈 응답 반환
                return Mono.just(new VocabularyAgentResponse("vocabulary", List.of()));
            });
    }
    
    /**
     * chatroomId를 통해 컨셉과 레벨을 조회하여 어휘 추출
     * 
     * @param botResponse 챗봇 응답 문장
     * @param chatroomId 채팅방 ID
     * @return 어휘 추출 결과
     */
    public Mono<VocabularyAgentResponse> extractDifficultWords(String botResponse, UUID chatroomId) {
        log.info("=== VocabularyAgent.extractDifficultWords() 호출됨 (Facade with chatroomId) ===");
        log.info("=== 파라미터 - botResponse='{}', chatroomId={} ===", botResponse, chatroomId);
        
        // ChatService를 통해 concept과 intimacyLevel 조회
        String concept = chatService.getConcept(chatroomId);
        Integer intimacyLevel = chatService.getIntimacyLevel(chatroomId);
        
        log.info("VocabularyAgent: chatroomId로 조회 - concept='{}', intimacyLevel={}", 
            concept, intimacyLevel);
        
        return extractDifficultWords(botResponse, concept, intimacyLevel);
    }
}
