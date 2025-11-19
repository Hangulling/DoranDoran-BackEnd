package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.enums.ChatRoomConcept;
import com.dorandoran.chat.repository.ChatbotRepository;
import com.dorandoran.chat.service.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Excel 데이터를 기반으로 프롬프트를 생성하고 업데이트하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptGenerationService {

    private final ExcelReaderService excelReaderService;
    private final ChatbotService chatbotService;
    private final ChatbotRepository chatbotRepository;
    private final ObjectMapper objectMapper;

    // Excel 데이터 캐시
    private List<IntimacyExampleDto> intimacyExamples;
    private List<VocabularyExampleDto> vocabularyExamples;
    private List<ConversationExampleDto> conversationExamples;
    private List<GreetingExampleDto> greetingExamples;

    /**
     * 애플리케이션 시작 시 모든 챗봇 프롬프트 업데이트
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void updateAllChatbotsOnStartup() {
        log.info("=== Excel 기반 프롬프트 업데이트 시작 ===");
        
        try {
            // Excel 파일 읽기
            loadExcelData();
            
            // 모든 활성 챗봇 조회
            List<Chatbot> activeChatbots = chatbotRepository.findAll().stream()
                .filter(bot -> bot.getIsActive() == null || bot.getIsActive())
                .collect(Collectors.toList());
            
            log.info("활성 챗봇 수: {}", activeChatbots.size());
            
            // 각 챗봇별로 프롬프트 업데이트
            int successCount = 0;
            int failCount = 0;
            
            for (Chatbot chatbot : activeChatbots) {
                try {
                    updateChatbotPrompts(chatbot);
                    successCount++;
                } catch (Exception e) {
                    log.error("챗봇 프롬프트 업데이트 실패: chatbotId={}", chatbot.getId(), e);
                    failCount++;
                }
            }
            
            log.info("=== Excel 기반 프롬프트 업데이트 완료: 성공={}, 실패={} ===", successCount, failCount);
            
        } catch (Exception e) {
            log.error("프롬프트 업데이트 중 오류 발생", e);
        }
    }

    /**
     * Excel 데이터 로드
     */
    private void loadExcelData() {
        log.info("Excel 파일 읽기 시작...");
        intimacyExamples = excelReaderService.readIntimacyExamples();
        vocabularyExamples = excelReaderService.readVocabularyExamples();
        conversationExamples = excelReaderService.readConversationExamples();
        greetingExamples = excelReaderService.readGreetingExamples();
        log.info("Excel 파일 읽기 완료: Intimacy={}, Vocabulary={}, Conversation={}, Greeting={}",
            intimacyExamples.size(), vocabularyExamples.size(), 
            conversationExamples.size(), greetingExamples.size());
    }

    /**
     * 특정 챗봇의 프롬프트 업데이트
     */
    @Transactional
    public void updateChatbotPrompts(Chatbot chatbot) {
        log.info("챗봇 프롬프트 업데이트 시작: chatbotId={}, name={}", chatbot.getId(), chatbot.getName());
        
        // 챗봇의 concept과 intimacyLevel 확인
        String concept = extractConceptFromChatbot(chatbot);
        Integer intimacyLevel = chatbot.getIntimacyLevel() != null 
            ? chatbot.getIntimacyLevel() 
            : ChatRoomConcept.fromString(concept).getDefaultIntimacyLevel();
        
        log.info("챗봇 정보: concept={}, intimacyLevel={}", concept, intimacyLevel);
        
        // Excel 데이터가 아직 로드되지 않았다면 로드
        if (intimacyExamples == null) {
            loadExcelData();
        }
        
        // 각 Agent별 프롬프트 업데이트
        updateIntimacyPrompt(chatbot, concept, intimacyLevel);
        updateVocabularyPrompt(chatbot, concept);
        updateConversationPrompt(chatbot, concept, intimacyLevel);
        
        log.info("챗봇 프롬프트 업데이트 완료: chatbotId={}", chatbot.getId());
    }

    /**
     * Greeting 예시 가져오기 (GreetingService에서 사용)
     */
    public List<GreetingExampleDto> getGreetingExamples(String concept, Integer intimacyLevel) {
        if (greetingExamples == null) {
            loadExcelData();
        }
        return filterGreetingExamples(concept, intimacyLevel);
    }

    /**
     * Greeting 예시 필터링
     */
    private List<GreetingExampleDto> filterGreetingExamples(String concept, Integer intimacyLevel) {
        return greetingExamples.stream()
            .filter(ex -> matchesConcept(ex.getConcept(), concept))
            .filter(ex -> ex.getIntimacyLevel() != null && ex.getIntimacyLevel().equals(intimacyLevel))
            .filter(ex -> ex.getBotMessage() != null && !ex.getBotMessage().trim().isEmpty())
            .limit(10) // 최대 10개
            .collect(Collectors.toList());
    }

    /**
     * Greeting 예시 섹션 생성
     */
    public String buildGreetingExamplesSection(String concept, Integer intimacyLevel) {
        List<GreetingExampleDto> examples = getGreetingExamples(concept, intimacyLevel);
        
        if (examples.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[인사말 예시]\n\n");
        
        for (int i = 0; i < examples.size(); i++) {
            GreetingExampleDto ex = examples.get(i);
            sb.append("예시 ").append(i + 1).append(":\n");
            if (ex.getTopic() != null && !ex.getTopic().trim().isEmpty()) {
                sb.append("주제: ").append(ex.getTopic()).append("\n");
            }
            sb.append("봇 메시지: ").append(ex.getBotMessage()).append("\n");
            if (ex.getGuideMessage() != null && !ex.getGuideMessage().trim().isEmpty()) {
                sb.append("가이드: ").append(ex.getGuideMessage()).append("\n");
            }
            sb.append("---\n\n");
        }
        
        return sb.toString();
    }

    /**
     * IntimacyAgent 프롬프트 업데이트
     */
    private void updateIntimacyPrompt(Chatbot chatbot, String concept, Integer intimacyLevel) {
        // 필터링된 예시 가져오기
        List<IntimacyExampleDto> filteredExamples = filterIntimacyExamples(concept, intimacyLevel);
        
        if (filteredExamples.isEmpty()) {
            log.warn("Intimacy 예시가 없습니다: concept={}, intimacyLevel={}", concept, intimacyLevel);
            return;
        }
        
        // 기존 프롬프트 가져오기
        String basePrompt = chatbot.getIntimacySystemPrompt();
        if (basePrompt == null || basePrompt.trim().isEmpty()) {
            log.warn("Intimacy 기본 프롬프트가 없습니다. chatbotId={}", chatbot.getId());
            return;
        }
        
        // 예시 섹션 생성
        String examplesSection = buildIntimacyExamplesSection(filteredExamples);
        
        // 프롬프트 업데이트
        String updatedPrompt = basePrompt + "\n\n" + examplesSection;
        
        // DB 업데이트
        ChatbotUpdateRequest request = new ChatbotUpdateRequest();
        request.setChatbotId(chatbot.getId().toString());
        request.setAgentType("intimacy");
        request.setSystemPrompt(updatedPrompt);
        
        boolean success = chatbotService.updateChatbotPrompt(request);
        if (!success) {
            throw new RuntimeException("Intimacy 프롬프트 업데이트 실패: chatbotId=" + chatbot.getId());
        }
        
        log.info("Intimacy 프롬프트 업데이트 완료: chatbotId={}, 예시 수={}", chatbot.getId(), filteredExamples.size());
    }

    /**
     * VocabularyAgent 프롬프트 업데이트
     * 
     * @deprecated 이 메서드는 더 이상 사용되지 않습니다.
     * VocabularyExtractionAgent와 VocabularyExplanationAgent는 하드코딩된 프롬프트를 사용하며,
     * DB의 vocabulary_system_prompt를 읽지 않습니다.
     * 
     * 이 메서드는 DB에 프롬프트를 업데이트하지만, 실제 Agent는 이를 무시하고
     * VocabularyExtractionAgent.buildExtractionPrompt()와
     * VocabularyExplanationAgent.buildExplanationPrompt()에서 생성한 하드코딩된 프롬프트를 사용합니다.
     * 
     * DB의 vocabulary_system_prompt는 resetChatbotPrompt()에서만 사용됩니다 (기본값 리셋용).
     * 
     * @param chatbot 챗봇 엔티티
     * @param concept 컨셉 (FRIEND, COWORKER 등)
     */
    @Deprecated
    private void updateVocabularyPrompt(Chatbot chatbot, String concept) {
        // 필터링된 예시 가져오기
        List<VocabularyExampleDto> filteredExamples = filterVocabularyExamples(concept);
        
        if (filteredExamples.isEmpty()) {
            log.warn("Vocabulary 예시가 없습니다: concept={}", concept);
            return;
        }
        
        // 기존 프롬프트 가져오기
        String basePrompt = chatbot.getVocabularySystemPrompt();
        if (basePrompt == null || basePrompt.trim().isEmpty()) {
            log.warn("Vocabulary 기본 프롬프트가 없습니다. chatbotId={}", chatbot.getId());
            return;
        }
        
        // 예시 섹션 생성
        String examplesSection = buildVocabularyExamplesSection(filteredExamples);
        
        // 프롬프트 업데이트
        String updatedPrompt = basePrompt + "\n\n" + examplesSection;
        
        // DB 업데이트
        ChatbotUpdateRequest request = new ChatbotUpdateRequest();
        request.setChatbotId(chatbot.getId().toString());
        request.setAgentType("vocabulary");
        request.setSystemPrompt(updatedPrompt);
        
        boolean success = chatbotService.updateChatbotPrompt(request);
        if (!success) {
            throw new RuntimeException("Vocabulary 프롬프트 업데이트 실패: chatbotId=" + chatbot.getId());
        }
        
        log.info("Vocabulary 프롬프트 업데이트 완료: chatbotId={}, 예시 수={}", chatbot.getId(), filteredExamples.size());
    }

    /**
     * ConversationAgent 프롬프트 업데이트
     */
    private void updateConversationPrompt(Chatbot chatbot, String concept, Integer intimacyLevel) {
        // 필터링된 예시 가져오기
        List<ConversationExampleDto> filteredExamples = filterConversationExamples(concept, intimacyLevel);
        
        if (filteredExamples.isEmpty()) {
            log.warn("Conversation 예시가 없습니다: concept={}, intimacyLevel={}", concept, intimacyLevel);
            return;
        }
        
        // 기존 프롬프트 가져오기
        String basePrompt = chatbot.getSystemPrompt();
        if (basePrompt == null || basePrompt.trim().isEmpty()) {
            log.warn("Conversation 기본 프롬프트가 없습니다. chatbotId={}", chatbot.getId());
            return;
        }
        
        // 예시 섹션 생성
        String examplesSection = buildConversationExamplesSection(filteredExamples);
        
        // 프롬프트 업데이트
        String updatedPrompt = basePrompt + "\n\n" + examplesSection;
        
        // DB 업데이트
        ChatbotUpdateRequest request = new ChatbotUpdateRequest();
        request.setChatbotId(chatbot.getId().toString());
        request.setAgentType("conversation");
        request.setSystemPrompt(updatedPrompt);
        
        boolean success = chatbotService.updateChatbotPrompt(request);
        if (!success) {
            throw new RuntimeException("Conversation 프롬프트 업데이트 실패: chatbotId=" + chatbot.getId());
        }
        
        log.info("Conversation 프롬프트 업데이트 완료: chatbotId={}, 예시 수={}", chatbot.getId(), filteredExamples.size());
    }

    /**
     * Intimacy 예시 필터링
     */
    private List<IntimacyExampleDto> filterIntimacyExamples(String concept, Integer intimacyLevel) {
        return intimacyExamples.stream()
            .filter(ex -> matchesConcept(ex.getConcept(), concept))
            .filter(ex -> ex.getIntimacyLevel() != null && ex.getIntimacyLevel().equals(intimacyLevel))
            .filter(ex -> ex.getUserMessage() != null && !ex.getUserMessage().trim().isEmpty())
            .limit(20) // 최대 20개
            .collect(Collectors.toList());
    }

    /**
     * Vocabulary 예시 필터링
     */
    private List<VocabularyExampleDto> filterVocabularyExamples(String concept) {
        return vocabularyExamples.stream()
            .filter(ex -> matchesConcept(ex.getRelation(), concept))
            .filter(ex -> ex.getWord() != null && !ex.getWord().trim().isEmpty())
            .limit(20) // 최대 20개
            .collect(Collectors.toList());
    }

    /**
     * Conversation 예시 필터링
     */
    private List<ConversationExampleDto> filterConversationExamples(String concept, Integer intimacyLevel) {
        return conversationExamples.stream()
            .filter(ex -> matchesConcept(ex.getConcept(), concept))
            .filter(ex -> ex.getIntimacyLevel() != null && ex.getIntimacyLevel().equals(intimacyLevel))
            .filter(ex -> ex.getUserMessage() != null && !ex.getUserMessage().trim().isEmpty())
            .limit(20) // 최대 20개
            .collect(Collectors.toList());
    }

    /**
     * Concept 매칭 (대소문자 무시)
     */
    private boolean matchesConcept(String excelConcept, String chatbotConcept) {
        if (excelConcept == null || chatbotConcept == null) {
            return false;
        }
        return excelConcept.trim().equalsIgnoreCase(chatbotConcept.trim());
    }

    /**
     * Intimacy 예시 섹션 생성
     */
    private String buildIntimacyExamplesSection(List<IntimacyExampleDto> examples) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Few-shot 예시]\n\n");
        
        for (int i = 0; i < examples.size(); i++) {
            IntimacyExampleDto ex = examples.get(i);
            sb.append("예시 ").append(i + 1).append(":\n");
            sb.append("- 사용자 입력: ").append(ex.getUserMessage()).append("\n");
            if (ex.getIntimacyLevel() != null) {
                sb.append("- 감지된 레벨: ").append(ex.getIntimacyLevel()).append("\n");
            }
            if (ex.getCorrectedSentence() != null && !ex.getCorrectedSentence().trim().isEmpty()) {
                sb.append("- 교정된 문장: ").append(ex.getCorrectedSentence()).append("\n");
            }
            if (ex.getKo() != null && !ex.getKo().trim().isEmpty()) {
                sb.append("- 피드백(ko): ").append(ex.getKo()).append("\n");
            }
            if (ex.getEn() != null && !ex.getEn().trim().isEmpty()) {
                sb.append("- 피드백(en): ").append(ex.getEn()).append("\n");
            }
            sb.append("---\n\n");
        }
        
        return sb.toString();
    }

    /**
     * Vocabulary 예시 섹션 생성
     */
    private String buildVocabularyExamplesSection(List<VocabularyExampleDto> examples) {
        StringBuilder sb = new StringBuilder();
        sb.append("[참고 예시]\n\n");
        
        for (int i = 0; i < examples.size(); i++) {
            VocabularyExampleDto ex = examples.get(i);
            sb.append("예시 ").append(i + 1).append(":\n");
            if (ex.getContent() != null && !ex.getContent().trim().isEmpty()) {
                sb.append("- 문장: ").append(ex.getContent()).append("\n");
            }
            sb.append("- 추출 단어: ").append(ex.getWord());
            if (ex.getDifficulty() != null) {
                sb.append(" (난이도: ").append(ex.getDifficulty()).append(")");
            }
            sb.append("\n");
            if (ex.getRoma() != null && !ex.getRoma().trim().isEmpty()) {
                sb.append("- 로마자: ").append(ex.getRoma()).append("\n");
            }
            if (ex.getKo() != null && !ex.getKo().trim().isEmpty()) {
                sb.append("- 설명(ko): ").append(ex.getKo()).append("\n");
            }
            if (ex.getEn() != null && !ex.getEn().trim().isEmpty()) {
                sb.append("- 설명(en): ").append(ex.getEn()).append("\n");
            }
            sb.append("---\n\n");
        }
        
        return sb.toString();
    }

    /**
     * Conversation 예시 섹션 생성
     */
    private String buildConversationExamplesSection(List<ConversationExampleDto> examples) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Few-shot 대화 예시]\n\n");
        
        for (int i = 0; i < examples.size(); i++) {
            ConversationExampleDto ex = examples.get(i);
            sb.append("예시 ").append(i + 1).append(":\n");
            if (ex.getSituation() != null && !ex.getSituation().trim().isEmpty()) {
                sb.append("상황: ").append(ex.getSituation()).append("\n");
            }
            sb.append("사용자: ").append(ex.getUserMessage()).append("\n");
            sb.append("봇: ").append(ex.getBotMessage()).append("\n");
            sb.append("---\n\n");
        }
        
        return sb.toString();
    }

    /**
     * 챗봇의 concept 추출
     */
    private String extractConceptFromChatbot(Chatbot chatbot) {
        try {
            if (chatbot.getSettings() != null && !chatbot.getSettings().trim().isEmpty()) {
                JsonNode settings = objectMapper.readTree(chatbot.getSettings());
                if (settings.has("concept")) {
                    JsonNode conceptNode = settings.get("concept");
                    if (conceptNode.isTextual()) {
                        // 대문자로 정규화하여 반환 (일관성 유지)
                        return conceptNode.asText().toUpperCase();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("챗봇 settings에서 concept 추출 실패: chatbotId={}", chatbot.getId(), e);
        }
        
        // 기본값 반환
        return "FRIEND";
    }
}

