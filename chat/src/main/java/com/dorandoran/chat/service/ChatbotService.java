package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.repository.ChatbotRepository;
import com.dorandoran.chat.service.dto.ChatbotUpdateRequest;
import com.dorandoran.chat.dto.ChatbotDirectivesRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 챗봇 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {
    
    private final ChatbotRepository chatbotRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PromptService promptService;
    
    /**
     * 챗봇 프롬프트 업데이트
     */
    @Transactional
    public boolean updateChatbotPrompt(ChatbotUpdateRequest request) {
        try {
            UUID chatbotId = UUID.fromString(request.getChatbotId());
            
            // 챗봇 존재 확인
            Optional<Chatbot> chatbotOpt = chatbotRepository.findById(chatbotId);
            if (chatbotOpt.isEmpty()) {
                log.error("챗봇을 찾을 수 없습니다: {}", chatbotId);
                return false;
            }
            
            // 각 Agent별 프롬프트 업데이트
            String sql = null;
            Object[] params = null;
            
            if ("conversation".equals(request.getAgentType())) {
                if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
                    sql = "UPDATE chat_schema.chatbots SET system_prompt = ?, updated_at = ? WHERE id = ?";
                    params = new Object[]{request.getSystemPrompt().trim(), LocalDateTime.now(), chatbotId};
                }
            } else if ("intimacy".equals(request.getAgentType())) {
                sql = "UPDATE chat_schema.chatbots SET intimacy_system_prompt = ?, intimacy_user_prompt = ?, updated_at = ? WHERE id = ?";
                params = new Object[]{
                    request.getSystemPrompt() != null ? request.getSystemPrompt().trim() : null,
                    request.getUserPrompt() != null ? request.getUserPrompt().trim() : null,
                    LocalDateTime.now(), 
                    chatbotId
                };
            } else if ("vocabulary".equals(request.getAgentType())) {
                sql = "UPDATE chat_schema.chatbots SET vocabulary_system_prompt = ?, vocabulary_user_prompt = ?, updated_at = ? WHERE id = ?";
                params = new Object[]{
                    request.getSystemPrompt() != null ? request.getSystemPrompt().trim() : null,
                    request.getUserPrompt() != null ? request.getUserPrompt().trim() : null,
                    LocalDateTime.now(), 
                    chatbotId
                };
            } else if ("translation".equals(request.getAgentType())) {
                sql = "UPDATE chat_schema.chatbots SET translation_system_prompt = ?, translation_user_prompt = ?, updated_at = ? WHERE id = ?";
                params = new Object[]{
                    request.getSystemPrompt() != null ? request.getSystemPrompt().trim() : null,
                    request.getUserPrompt() != null ? request.getUserPrompt().trim() : null,
                    LocalDateTime.now(), 
                    chatbotId
                };
            }
            
            if (sql != null && params != null) {
                int updated = jdbcTemplate.update(sql, params);
                
                if (updated > 0) {
                    log.info("{} Agent 프롬프트 업데이트 완료: {}", request.getAgentType(), chatbotId);
                    return true;
                } else {
                    log.error("{} Agent 프롬프트 업데이트 실패: {}", request.getAgentType(), chatbotId);
                    return false;
                }
            }
            
            // 다른 Agent들은 현재 하드코딩되어 있어서 별도 처리 필요
            // TODO: 다른 Agent들도 데이터베이스에서 관리하도록 개선 필요
            
            log.info("챗봇 프롬프트 업데이트 완료: {}", chatbotId);
            return true;
            
        } catch (Exception e) {
            log.error("챗봇 프롬프트 업데이트 실패: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 챗봇 기본값으로 리셋
     */
    @Transactional
    public boolean resetChatbotPrompt(String chatbotId, String agentType) {
        try {
            UUID id = UUID.fromString(chatbotId);
            
            // 챗봇 존재 확인
            Optional<Chatbot> chatbotOpt = chatbotRepository.findById(id);
            if (chatbotOpt.isEmpty()) {
                log.error("챗봇을 찾을 수 없습니다: {}", id);
                return false;
            }
            
            // 각 Agent별 기본값으로 리셋
            String sql = null;
            Object[] params = null;
            
            if ("conversation".equals(agentType)) {
                String defaultPrompt = getDefaultConversationPrompt();
                sql = "UPDATE chat_schema.chatbots SET system_prompt = ?, updated_at = ? WHERE id = ?";
                params = new Object[]{defaultPrompt, LocalDateTime.now(), id};
            } else if ("intimacy".equals(agentType)) {
                String[] defaultPrompts = getDefaultIntimacyPrompts();
                sql = "UPDATE chat_schema.chatbots SET intimacy_system_prompt = ?, intimacy_user_prompt = ?, updated_at = ? WHERE id = ?";
                params = new Object[]{defaultPrompts[0], defaultPrompts[1], LocalDateTime.now(), id};
            } else if ("vocabulary".equals(agentType)) {
                String[] defaultPrompts = getDefaultVocabularyPrompts();
                sql = "UPDATE chat_schema.chatbots SET vocabulary_system_prompt = ?, vocabulary_user_prompt = ?, updated_at = ? WHERE id = ?";
                params = new Object[]{defaultPrompts[0], defaultPrompts[1], LocalDateTime.now(), id};
            } else if ("translation".equals(agentType)) {
                // TranslationAgent 제거됨 - VocabularyAgent가 모든 기능을 담당
                log.warn("TranslationAgent는 더 이상 지원되지 않습니다. VocabularyAgent를 사용하세요.");
                return false;
            }
            
            if (sql != null && params != null) {
                int updated = jdbcTemplate.update(sql, params);
                
                if (updated > 0) {
                    log.info("{} Agent 기본값으로 리셋 완료: {}", agentType, id);
                    return true;
                } else {
                    log.error("{} Agent 리셋 실패: {}", agentType, id);
                    return false;
                }
            }
            
            log.info("챗봇 프롬프트 리셋 완료: {}", id);
            return true;
            
        } catch (Exception e) {
            log.error("챗봇 프롬프트 리셋 실패: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 기본 ConversationAgent 프롬프트
     */
    private String getDefaultConversationPrompt() {
        return "당신은 도란도란의 AI 어시스턴트입니다. 외국인 한국어 학습자를 도와주는 친근한 튜터입니다.\n\n" +
               "주요 역할:\n" +
               "- 한국어 학습에 대한 질문에 정확하고 도움이 되는 답변 제공\n" +
               "- 학습자의 레벨에 맞는 적절한 언어 사용\n" +
               "- 격려와 동기부여를 통한 학습 지원\n" +
               "- 한국 문화와 언어의 맥락 설명\n\n" +
               "응답 스타일:\n" +
               "- 친근하고 격려적인 톤\n" +
               "- 명확하고 이해하기 쉬운 설명\n" +
               "- 구체적인 예시 제공\n" +
               "- 학습자의 진전을 인정하고 격려\n\n" +
               "- 응답은 한국어로, 핵심 위주로 간결하게 작성하세요.";
    }

    /**
     * 기본 IntimacyAgent 프롬프트
     */
    private String[] getDefaultIntimacyPrompts() {
        String systemPrompt = "당신은 외국인의 한국어 친밀도를 분석하는 전문가입니다.\n\n" +
                "분석 기준:\n" +
                "- 1단계 (격식체/존댓말): \"~습니다\", \"~입니다\", \"~하겠습니다\"\n" +
                "- 2단계 (부드러운 존댓말): \"~해요\", \"~이에요\", \"~있어요\"\n" +
                "- 3단계 (친근한 반말): \"~야\", \"~어\", \"~지\"\n\n" +
                "현재 학습자의 목표 레벨: {level} (1=격식체/존댓말, 2=부드러운 존댓말, 3=친근한 반말)\n\n" +
                "다음 문장을 분석하여 JSON 형식으로 답변하세요:\n" +
                "{\n" +
                "  \"detectedLevel\": 1-3,\n" +
                "  \"correctedSentence\": \"교정된 문장\",\n" +
                "  \"feedback\": \"피드백 메시지\",\n" +
                "  \"corrections\": [\"변경사항1\", \"변경사항2\"]\n" +
                "}";
        String userPrompt = "다음 한국어 문장의 친밀도를 분석하고, 더 적절한 친밀도로 수정해주세요: {input}";
        return new String[]{systemPrompt, userPrompt};
    }

    /**
     * 기본 VocabularyAgent 프롬프트
     */
    private String[] getDefaultVocabularyPrompts() {
        String systemPrompt = "외국인이 이해하기 어려운 한국어 단어/표현을 최대 1개 추출하세요. 반드시 1개만 추출하세요.\n\n" +
                "추출 기준:\n" +
                "- 문법적으로 복잡한 구조\n" +
                "- 한국어 특유의 표현\n" +
                "- 문화적 맥락이 필요한 단어\n" +
                "- 학습자 레벨에 맞는 적절한 난이도\n\n" +
                "사용자 레벨: {userLevel} (1=초급, 2=중급, 3=고급)\n\n" +
                "JSON 형식:\n" +
                "{\n" +
                "  \"words\": [\n" +
                "    {\"word\": \"단어\", \"difficulty\": 1-3, \"context\": \"문맥\"}\n" +
                "  ]\n" +
                "}";
        String userPrompt = "다음 상황에서 사용할 수 있는 적절한 한국어 어휘를 추천해주세요: {input}";
        return new String[]{systemPrompt, userPrompt};
    }

    // TranslationAgent 제거됨 - VocabularyAgent가 모든 기능을 담당
    
    /**
     * 챗봇 조회
     */
    public Optional<Chatbot> getChatbot(String chatbotId) {
        try {
            UUID id = UUID.fromString(chatbotId);
            return chatbotRepository.findById(id);
        } catch (Exception e) {
            log.error("챗봇 조회 실패: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 챗봇 프롬프트 조회 (단일 프롬프트)
     */
    public String getChatbotPrompt(String chatbotId, String agentType) {
        Chatbot chatbot = chatbotRepository.findById(UUID.fromString(chatbotId))
            .orElseThrow(() -> new RuntimeException("Chatbot not found: " + chatbotId));
        
        return switch (agentType.toLowerCase()) {
            case "conversation" -> chatbot.getSystemPrompt();
            case "intimacy" -> chatbot.getIntimacySystemPrompt();
            case "vocabulary" -> chatbot.getVocabularySystemPrompt();
            case "translation" -> chatbot.getTranslationSystemPrompt();
            default -> throw new IllegalArgumentException("Unknown agent type: " + agentType);
        };
    }

    /**
     * 특정 Agent의 프롬프트 조회
     */
    public Map<String, String> getAgentPrompts(String chatbotId, String agentType) {
        try {
            UUID id = UUID.fromString(chatbotId);
            Optional<Chatbot> chatbotOpt = chatbotRepository.findById(id);
            
            if (chatbotOpt.isEmpty()) {
                log.error("챗봇을 찾을 수 없습니다: {}", id);
                return Map.of();
            }
            
            Chatbot chatbot = chatbotOpt.get();
            Map<String, String> prompts = new HashMap<>();
            
            switch (agentType.toLowerCase()) {
                case "conversation":
                    prompts.put("systemPrompt", chatbot.getSystemPrompt());
                    break;
                case "intimacy":
                    prompts.put("systemPrompt", chatbot.getIntimacySystemPrompt());
                    prompts.put("userPrompt", chatbot.getIntimacyUserPrompt());
                    break;
                case "vocabulary":
                    prompts.put("systemPrompt", chatbot.getVocabularySystemPrompt());
                    prompts.put("userPrompt", chatbot.getVocabularyUserPrompt());
                    break;
                case "translation":
                    prompts.put("systemPrompt", chatbot.getTranslationSystemPrompt());
                    prompts.put("userPrompt", chatbot.getTranslationUserPrompt());
                    break;
                default:
                    log.warn("지원하지 않는 AgentType: {}", agentType);
                    return Map.of();
            }
            
            return prompts;
            
        } catch (Exception e) {
            log.error("Agent 프롬프트 조회 실패: {}", e.getMessage(), e);
            return Map.of();
        }
    }
    
    /**
     * 전체 프롬프트 조회 (Base + Dynamic)
     */
    public String getFullPromptForAgent(String chatbotId, String agentType, UUID chatroomId) {
        return switch (agentType.toLowerCase()) {
            case "conversation" -> promptService.buildFullConversationPrompt(chatroomId);
            case "intimacy" -> promptService.buildFullIntimacyPrompt(chatroomId);
            case "vocabulary", "translation" -> getChatbotPrompt(chatbotId, agentType);
            default -> throw new IllegalArgumentException("Unknown agent type: " + agentType);
        };
    }

    /**
     * Base 프롬프트만 조회 (Dynamic Directives 제외)
     */
    public String getBasePromptForAgent(String chatbotId, String agentType, UUID chatroomId) {
        return switch (agentType.toLowerCase()) {
            case "conversation" -> promptService.getConversationBasePrompt(chatroomId);
            case "intimacy" -> promptService.getIntimacyBasePrompt(chatroomId);
            case "vocabulary", "translation" -> getChatbotPrompt(chatbotId, agentType);
            default -> throw new IllegalArgumentException("Unknown agent type: " + agentType);
        };
    }

    /**
     * 챗봇 Directives 설정 업데이트
     */
    @Transactional
    public boolean updateChatbotDirectives(String chatbotId, ChatbotDirectivesRequest request) {
        try {
            UUID id = UUID.fromString(chatbotId);
            Optional<Chatbot> chatbotOpt = chatbotRepository.findById(id);
            
            if (chatbotOpt.isEmpty()) {
                log.error("챗봇을 찾을 수 없습니다: {}", chatbotId);
                return false;
            }
            
            Chatbot chatbot = chatbotOpt.get();
            ObjectMapper mapper = new ObjectMapper();
            
            // 기존 settings 파싱
            ObjectNode settings;
            if (chatbot.getSettings() != null && !chatbot.getSettings().isBlank()) {
                settings = (ObjectNode) mapper.readTree(chatbot.getSettings());
            } else {
                settings = mapper.createObjectNode();
            }
            
            // directives 노드 생성/업데이트
            ObjectNode directives = settings.has("directives") 
                ? (ObjectNode) settings.get("directives") 
                : settings.putObject("directives");
            
            // concept
            if (request.getConcept() != null) {
                ObjectNode concept = directives.putObject("concept");
                if (request.getConcept().getEnabled() != null) {
                    concept.put("enabled", request.getConcept().getEnabled());
                }
                if (request.getConcept().getCustom() != null) {
                    concept.put("custom", request.getConcept().getCustom());
                }
            }
            
            // intimacy
            if (request.getIntimacy() != null) {
                ObjectNode intimacy = directives.putObject("intimacy");
                if (request.getIntimacy().getEnabled() != null) {
                    intimacy.put("enabled", request.getIntimacy().getEnabled());
                }
                if (request.getIntimacy().getCustom() != null) {
                    intimacy.put("custom", request.getIntimacy().getCustom());
                }
            }
            
            // language
            if (request.getLanguage() != null) {
                ObjectNode language = directives.putObject("language");
                if (request.getLanguage().getEnabled() != null) {
                    language.put("enabled", request.getLanguage().getEnabled());
                }
                if (request.getLanguage().getDefaultLang() != null) {
                    language.put("default", request.getLanguage().getDefaultLang());
                }
                if (request.getLanguage().getAllowUserOverride() != null) {
                    language.put("allowUserOverride", request.getLanguage().getAllowUserOverride());
                }
            }
            
            // 저장
            chatbot.setSettings(settings.toString());
            chatbotRepository.save(chatbot);
            
            log.info("Directives 업데이트 성공: chatbotId={}", chatbotId);
            return true;
        } catch (Exception e) {
            log.error("Directives 업데이트 실패: chatbotId={}", chatbotId, e);
            return false;
        }
    }

    /**
     * 챗봇 Directives 설정 조회
     */
    public Map<String, Object> getChatbotDirectives(String chatbotId) {
        try {
            UUID id = UUID.fromString(chatbotId);
            Optional<Chatbot> chatbotOpt = chatbotRepository.findById(id);
            
            if (chatbotOpt.isEmpty()) {
                log.warn("챗봇을 찾을 수 없습니다: {}", chatbotId);
                return Collections.emptyMap();
            }
            
            Chatbot chatbot = chatbotOpt.get();
            if (chatbot.getSettings() == null || chatbot.getSettings().isBlank()) {
                log.info("챗봇 settings가 비어있습니다: {}", chatbotId);
                return Collections.emptyMap();
            }
            
            ObjectMapper mapper = new ObjectMapper();
            var settings = mapper.readTree(chatbot.getSettings());
            
            if (settings.has("directives")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = mapper.convertValue(settings.get("directives"), Map.class);
                return result;
            }
            
            log.info("Directives 설정이 없습니다: {}", chatbotId);
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Directives 조회 실패: chatbotId={}", chatbotId, e);
            return Collections.emptyMap();
        }
    }
}
