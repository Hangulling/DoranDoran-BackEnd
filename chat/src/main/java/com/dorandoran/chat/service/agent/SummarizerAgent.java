package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.service.ChatService;
import com.dorandoran.chat.service.OpenAIClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummarizerAgent {
    private final OpenAIClient openAIClient;
    private final ChatService chatService;

    public SummaryResult summarize(UUID chatroomId, int recentWindowSize, String previousSummaryCompact) {
        long startTime = System.currentTimeMillis();
        int inputTokens = 0;
        int outputTokens = 0;
        try {
            List<Message> all = chatService.listMessages(chatroomId);
            int from = Math.max(0, all.size() - recentWindowSize);
            List<Message> recent = new ArrayList<>(all.subList(from, all.size()));

            // PII 마스킹 적용
            List<Message> maskedRecent = recent.stream()
                .map(msg -> maskPII(msg))
                .collect(Collectors.toList());

            String system = buildSystemPrompt();
            String user = buildUserPrompt(maskedRecent, previousSummaryCompact);

            StringBuilder full = new StringBuilder();
            Flux<String> raw = openAIClient.streamRawCompletion(system, user);
            raw.flatMap(openAIClient::extractText)
               .doOnNext(full::append)
               .blockLast();

            // 토큰 수 추정 (대략적)
            inputTokens = estimateTokens(system + user);
            outputTokens = estimateTokens(full.toString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(full.toString());

            SummaryResult result = new SummaryResult();
            result.timestamp = LocalDateTime.now().toString();
            result.summary = node.has("summary") ? node.get("summary").toString() : "{}";
            result.tokens = inputTokens + outputTokens;
            result.keywords = new ArrayList<>();
            if (node.has("keywords") && node.get("keywords").isArray()) {
                for (JsonNode k : node.get("keywords")) {
                    String keyword = k.asText();
                    // 키워드 길이 제한 (50자)
                    if (keyword.length() > 50) {
                        keyword = keyword.substring(0, 47) + "...";
                    }
                    result.keywords.add(keyword);
                }
            }
            // 키워드 수 제한 (상위 10개)
            if (result.keywords.size() > 10) {
                result.keywords = result.keywords.subList(0, 10);
            }
            result.windowStartSeq = recent.isEmpty() ? 0 : Math.toIntExact(recent.get(0).getSequenceNumber());
            result.windowEndSeq = recent.isEmpty() ? 0 : Math.toIntExact(recent.get(recent.size() - 1).getSequenceNumber());

            long duration = System.currentTimeMillis() - startTime;
            log.info("SummarizerAgent 완료: chatroomId={}, duration={}ms, inputTokens={}, outputTokens={}, keywords={}", 
                chatroomId, duration, inputTokens, outputTokens, result.keywords.size());
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("SummarizerAgent 실패 - 비워진 결과 반환: chatroomId={}, duration={}ms", chatroomId, duration, e);
            SummaryResult empty = new SummaryResult();
            empty.timestamp = LocalDateTime.now().toString();
            empty.summary = "{}";
            empty.keywords = List.of();
            empty.tokens = 0;
            empty.windowStartSeq = 0;
            empty.windowEndSeq = 0;
            return empty;
        }
    }

    private String buildSystemPrompt() {
        return "당신은 대화 요약가입니다. 핵심 인물, 결정사항, 할 일, 선호, 사실을 구조적으로 요약하고, 상위 키워드를 반환하세요. 반드시 JSON만 반환하세요.";
    }

    private String buildUserPrompt(List<Message> recent, String previousSummaryCompact) {
        StringBuilder sb = new StringBuilder();
        sb.append("이전 요약(있으면 참고하되 덮어쓰지 말 것): ").append(previousSummaryCompact == null ? "{}" : previousSummaryCompact).append("\n\n");
        sb.append("최근 대화:\n");
        for (Message m : recent) {
            sb.append("[").append(m.getSequenceNumber()).append("] ")
              .append(m.getSenderType()).append(": ")
              .append(m.getContent()).append("\n");
        }
        sb.append("\nJSON 형식으로만 응답:\n{")
          .append("\"summary\": { \"participants\":[], \"decisions\":[], \"tasks\":[{\"title\":\"\",\"due\":null,\"status\":null}], \"preferences\":[], \"facts\":[] }, ")
          .append("\"keywords\": [\"키워드1\", \"키워드2\"] }");
        return sb.toString();
    }

    /**
     * PII 마스킹 (이메일, 전화번호, 개인정보)
     */
    private Message maskPII(Message message) {
        String content = message.getContent();
        if (content == null) return message;

        // 이메일 마스킹
        content = content.replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "[EMAIL]");
        
        // 전화번호 마스킹 (한국 형식)
        content = content.replaceAll("\\b(010|011|016|017|018|019)-?\\d{3,4}-?\\d{4}\\b", "[PHONE]");
        content = content.replaceAll("\\b\\d{2,3}-?\\d{3,4}-?\\d{4}\\b", "[PHONE]");
        
        // 주민등록번호 마스킹
        content = content.replaceAll("\\b\\d{6}-?\\d{7}\\b", "[ID_NUMBER]");
        
        // 카드번호 마스킹
        content = content.replaceAll("\\b\\d{4}-?\\d{4}-?\\d{4}-?\\d{4}\\b", "[CARD_NUMBER]");

        // 새 메시지 객체 생성 (불변성 유지)
        return Message.builder()
            .id(message.getId())
            .chatRoom(message.getChatRoom())
            .senderType(message.getSenderType())
            .senderId(message.getSenderId())
            .content(content)
            .contentType(message.getContentType())
            .sequenceNumber(message.getSequenceNumber())
            .isDeleted(message.getIsDeleted())
            .isEdited(message.getIsEdited())
            .createdAt(message.getCreatedAt())
            .updatedAt(message.getUpdatedAt())
            .build();
    }

    /**
     * 토큰 수 추정 (대략적)
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        // 한국어는 평균 1.5토큰/글자, 영어는 0.25토큰/글자로 추정
        int koreanChars = (int) text.chars().filter(c -> c >= 0xAC00 && c <= 0xD7AF).count();
        int otherChars = text.length() - koreanChars;
        return (int) (koreanChars * 1.5 + otherChars * 0.25);
    }

    public static class SummaryResult {
        public String timestamp;
        public String summary; // JSON 문자열
        public List<String> keywords;
        public int windowStartSeq;
        public int windowEndSeq;
        public int tokens;
    }
}


