package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.ChatbotRepository;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromptService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatbotRepository chatbotRepository;
    private final IntimacyProgressRepository intimacyProgressRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 룸의 context_data + 챗봇 system_prompt/personality/capabilities를 합성하여
     * 최종 시스템 프롬프트 문자열을 생성한다.
     */
    public String buildSystemPrompt(UUID chatroomId) {
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatroomId);
        if (roomOpt.isEmpty()) {
            return defaultSystemPrompt();
        }

        ChatRoom room = roomOpt.get();
        StringBuilder prompt = new StringBuilder();

        // 1) 챗봇 메타
        appendChatbotDirectives(room, prompt);

        // 2) 룸 컨텍스트 반영 (요약/선호/세션)
        appendRoomContext(room, prompt);
        
        // 3) 컨셉과 친밀도 기반 지시문 추가
        appendConceptAndIntimacyDirectives(room, prompt);

        // 4) 마무리 지시
        prompt.append("\n\n- 응답은 한국어로, 핵심 위주로 간결하게 작성하세요.\n");

        return truncate(prompt.toString(), 8000);
    }

    private void appendChatbotDirectives(ChatRoom room, StringBuilder prompt) {
        if (room.getChatbot() == null) return;
        Optional<Chatbot> botOpt = chatbotRepository.findById(room.getChatbot().getId());
        if (botOpt.isEmpty()) return;

        Chatbot bot = botOpt.get();

        // system_prompt
        if (bot.getSystemPrompt() != null && !bot.getSystemPrompt().isBlank()) {
            prompt.append(bot.getSystemPrompt().trim());
            prompt.append("\n\n");
        }

        // personality
        try {
            if (bot.getPersonality() != null && !bot.getPersonality().isBlank()) {
                JsonNode p = objectMapper.readTree(bot.getPersonality());
                // traits
                if (p.has("traits")) {
                    prompt.append("- 성격 특성: ");
                    prompt.append(joinArray(p.get("traits")));
                    prompt.append("\n");
                }
                // speakingStyle
                if (p.has("speakingStyle")) {
                    JsonNode s = p.get("speakingStyle");
                    if (s.has("honorific") && s.get("honorific").asBoolean()) {
                        prompt.append("- 존댓말을 사용하세요.\n");
                    }
                    if (s.has("formality")) {
                        prompt.append("- 말투 격식: ").append(s.get("formality").asText()).append("\n");
                    }
                    if (s.has("length")) {
                        prompt.append("- 답변 길이 선호: ").append(s.get("length").asText()).append("\n");
                    }
                }
                // guardrails
                if (p.has("guardrails")) {
                    JsonNode g = p.get("guardrails");
                    if (g.has("refuseTopics")) {
                        prompt.append("- 아래 주제는 답변을 정중히 거부하세요: ");
                        prompt.append(joinArray(g.get("refuseTopics"))).append("\n");
                    }
                    if (g.has("escalationHint")) {
                        prompt.append("- 필요 시 다음 안내를 덧붙이세요: ").append(g.get("escalationHint").asText()).append("\n");
                    }
                }
                // domainKnowledge
                if (p.has("domainKnowledge")) {
                    prompt.append("- 선호/전문 도메인: ");
                    prompt.append(joinArray(p.get("domainKnowledge"))).append("\n");
                }
                // fewShot 예시
                if (p.has("fewShot")) {
                    prompt.append("\n[예시 대화]\n");
                    for (JsonNode ex : p.get("fewShot")) {
                        JsonNode u = ex.get("user");
                        JsonNode a = ex.get("assistant");
                        if (u != null && a != null) {
                            prompt.append("사용자: ").append(u.asText()).append("\n");
                            prompt.append("어시스턴트: ").append(a.asText()).append("\n");
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // capabilities (응답 스타일 등)
        try {
            if (bot.getCapabilities() != null && !bot.getCapabilities().isBlank()) {
                JsonNode c = objectMapper.readTree(bot.getCapabilities());
                if (c.has("responseStyle")) {
                    JsonNode rs = c.get("responseStyle");
                    if (rs.has("format")) {
                        prompt.append("- 응답 포맷: ").append(rs.get("format").asText()).append("\n");
                    }
                    if (rs.has("bulletPreference")) {
                        prompt.append("- 불릿 사용: ").append(rs.get("bulletPreference").asText()).append("\n");
                    }
                    if (rs.has("maxLength")) {
                        prompt.append("- 최대 길이: ").append(rs.get("maxLength").asInt()).append("\n");
                    }
                }
                if (c.has("safety")) {
                    JsonNode s = c.get("safety");
                    if (s.has("profanityFilter") && s.get("profanityFilter").asBoolean()) {
                        prompt.append("- 욕설/비속어는 완곡하게 표현을 바꾸세요.\n");
                    }
                    if (s.has("piiRedaction") && s.get("piiRedaction").asBoolean()) {
                        prompt.append("- 개인정보는 식별 불가하게 마스킹하세요.\n");
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void appendRoomContext(ChatRoom room, StringBuilder prompt) {
        if (room.getContextData() == null || room.getContextData().isNull() || room.getContextData().isEmpty()) return;
        try {
            JsonNode ctx = room.getContextData();
            if (ctx.has("conversationSummary")) {
                prompt.append("\n[대화 요약]\n").append(ctx.get("conversationSummary").asText()).append("\n");
            }
            if (ctx.has("userPreferences")) {
                JsonNode pref = ctx.get("userPreferences");
                prompt.append("[사용자 선호]\n");
                if (pref.has("responseLength")) {
                    prompt.append("- 선호 응답 길이: ").append(pref.get("responseLength").asText()).append("\n");
                }
                if (pref.has("language")) {
                    prompt.append("- 언어: ").append(pref.get("language").asText()).append("\n");
                }
                if (pref.has("topics")) {
                    prompt.append("- 관심 주제: ").append(joinArray(pref.get("topics"))).append("\n");
                }
            }
            if (ctx.has("sessionData")) {
                JsonNode sd = ctx.get("sessionData");
                if (sd.has("currentTopic")) {
                    prompt.append("[현재 주제] ").append(sd.get("currentTopic").asText()).append("\n");
                }
            }
        } catch (Exception ignored) {}
    }

    private String joinArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) return "";
        StringBuilder sb = new StringBuilder();
        Iterator<JsonNode> it = arrayNode.elements();
        boolean first = true;
        while (it.hasNext()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(it.next().asText());
        }
        return sb.toString();
    }

    private String defaultSystemPrompt() {
        return "당신은 도란도란의 AI 어시스턴트입니다. 사용자의 질문에 간결하고 도움이 되게 답변하세요.";
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }
    
    private void appendConceptAndIntimacyDirectives(ChatRoom room, StringBuilder prompt) {
        // 컨셉 기반 지시문
        String concept = extractConceptFromSettings(room.getSettings());
        prompt.append("\n[대화 컨셉]\n");
        prompt.append(getConceptGuideline(concept));
        
        // 친밀도 기반 지시문
        int intimacyLevel = getCurrentIntimacyLevel(room.getId());
        prompt.append("\n[말투 지시]\n");
        prompt.append(getIntimacyGuideline(intimacyLevel));
    }
    
    private String extractConceptFromSettings(JsonNode settings) {
        if (settings != null && settings.has("concept")) {
            return settings.get("concept").asText();
        }
        return "FRIEND"; // 기본값
    }
    
    private int getCurrentIntimacyLevel(UUID chatroomId) {
        return intimacyProgressRepository.findByChatRoomId(chatroomId)
            .map(IntimacyProgress::getIntimacyLevel)
            .orElse(2); // 기본값
    }
    
    private String getConceptGuideline(String concept) {
        return switch (concept) {
            case "FRIEND" -> "- 친구처럼 편하고 자연스럽게 대화하세요\n- 가벼운 농담이나 친근한 표현을 사용해도 좋습니다";
            case "HONEY" -> "- 연인처럼 애정 어린 톤으로 대화하세요\n- 따뜻하고 사랑스러운 표현을 사용하세요";
            case "COWORKER" -> "- 직장 동료처럼 예의 바르고 전문적으로 대화하세요\n- 업무와 관련된 주제를 우선적으로 다루세요";
            case "SENIOR" -> "- 선배에게 대하듯 공손하고 정중하게 대화하세요\n- 존경과 예의를 바탕으로 한 대화를 하세요";
            default -> "- 일반적인 상황에 맞게 대화하세요";
        };
    }
    
    private String getIntimacyGuideline(int level) {
        return switch (level) {
            case 1 -> "- 격식체(~습니다, ~입니다)를 사용하세요\n- 정중하고 공손한 표현을 사용하세요";
            case 2 -> "- 부드러운 존댓말(~해요, ~이에요)을 사용하세요\n- 친근하면서도 예의 바른 표현을 사용하세요";
            case 3 -> "- 친근한 반말(~야, ~어, ~지)을 사용하세요\n- 편하고 자연스러운 표현을 사용하세요";
            default -> "- 적절한 말투로 대화하세요";
        };
    }
    
    /**
     * ConversationAgent의 전체 프롬프트 생성 (Base + Dynamic)
     */
    public String buildFullConversationPrompt(UUID chatroomId) {
        return buildSystemPrompt(chatroomId); // 기존 메서드 활용
    }

    /**
     * ConversationAgent의 Base Prompt만 조회
     */
    public String getConversationBasePrompt(UUID chatroomId) {
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatroomId);
        if (roomOpt.isEmpty()) {
            return defaultSystemPrompt();
        }
        
        ChatRoom room = roomOpt.get();
        if (room.getChatbot() == null) {
            return defaultSystemPrompt();
        }
        
        Optional<Chatbot> botOpt = chatbotRepository.findById(room.getChatbot().getId());
        return botOpt.map(Chatbot::getSystemPrompt).orElse(defaultSystemPrompt());
    }

    /**
     * IntimacyAgent의 전체 프롬프트 생성 (Base + Dynamic)
     */
    public String buildFullIntimacyPrompt(UUID chatroomId) {
        String basePrompt = getIntimacyBasePrompt(chatroomId);
        
        // Dynamic Directives
        ChatRoom room = chatRoomRepository.findById(chatroomId).orElse(null);
        if (room == null) return basePrompt;
        
        String concept = extractConceptFromSettings(room.getSettings());
        int level = getCurrentIntimacyLevel(chatroomId);
        
        String dynamicDirectives = String.format("""
            
            [분석 컨텍스트]
            현재 학습자의 목표 레벨: %d
            대화 컨셉: %s
            
            [컨셉별 지침]
            %s
            """, level, concept, getIntimacyConceptGuideline(concept));
        
        return basePrompt + dynamicDirectives;
    }

    /**
     * IntimacyAgent의 Base Prompt만 조회
     */
    public String getIntimacyBasePrompt(UUID chatroomId) {
        return chatRoomRepository.findById(chatroomId)
            .flatMap(room -> {
                if (room.getChatbot() == null) return Optional.empty();
                return chatbotRepository.findById(room.getChatbot().getId());
            })
            .map(Chatbot::getIntimacySystemPrompt)
            .orElse(getDefaultIntimacyBasePrompt());
    }

    private String getDefaultIntimacyBasePrompt() {
        return """
            당신은 외국인의 한국어 친밀도를 분석하는 전문가입니다.
            
            다음 문장을 분석하여 JSON 형식으로 답변하세요:
            {
              "detectedLevel": 1-3,
              "correctedSentence": "교정된 문장",
              "feedback": "피드백 메시지",
              "corrections": ["변경사항1", "변경사항2"]
            }
            """;
    }

    private String getIntimacyConceptGuideline(String concept) {
        return switch (concept) {
            case "FRIEND" -> "친구와의 대화 상황을 고려하여 자연스럽고 편한 표현을 교정하세요.";
            case "HONEY" -> "연인과의 대화 상황을 고려하여 애정 어린 표현을 교정하세요.";
            case "COWORKER" -> "직장 동료와의 대화 상황을 고려하여 예의 바르고 전문적인 표현을 교정하세요.";
            case "SENIOR" -> "선배와의 대화 상황을 고려하여 공손하고 정중한 표현을 교정하세요.";
            default -> "일반적인 상황에 맞는 적절한 표현을 교정하세요.";
        };
    }
}


