package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.ChatbotRepository;
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

        // 3) 마무리 지시
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
        if (room.getContextData() == null || room.getContextData().isBlank()) return;
        try {
            JsonNode ctx = objectMapper.readTree(room.getContextData());
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
}


