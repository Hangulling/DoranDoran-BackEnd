package com.dorandoran.chat.service.dto;

import com.dorandoran.chat.entity.Message;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Slf4j
public class MessageResponse {
    private UUID id;
    private UUID chatroomId;
    private String senderType;
    private UUID senderId;
    private String content;
    private String contentType;
    private Long sequenceNumber;
    private Boolean isEdited;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private MessageMetadata metadata;  // 구조화된 객체
    
    // === Nested Classes for Metadata ===
    
    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageMetadata {
        private UserMessageAnalysis userMessageAnalysis;
        private BotResponseAnalysis botResponseAnalysis;
    }
    
    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserMessageAnalysis {
        private String userMessageId;
        private IntimacyData intimacy;
    }
    
    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IntimacyData {
        private Integer detectedLevel;
        private String correctedSentence;
        private FeedbackText feedback;
        private String corrections;
    }
    
    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FeedbackText {
        private String ko;
        private String en;
    }
    
    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BotResponseAnalysis {
        private VocabularyData vocabulary;
    }
    
    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VocabularyData {
        private List<VocabularyWord> words;
    }
    
    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VocabularyWord {
        private String word;
        private Integer difficulty;
        private WordContext context;
    }
    
    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WordContext {
        private String roma;
        private String ko;
        private String en;
    }
    
    // === from() 메서드 ===
    
    public static MessageResponse from(Message m) {
        MessageMetadata metadata = null;
        
        // Bot 메시지의 metadata만 파싱
        if ("bot".equals(m.getSenderType()) && 
            m.getMetadata() != null && 
            !m.getMetadata().isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(m.getMetadata());
                
                // userMessageAnalysis 파싱
                UserMessageAnalysis userAnalysis = null;
                if (root.has("userMessageAnalysis")) {
                    JsonNode ua = root.get("userMessageAnalysis");
                    String userMessageId = ua.has("userMessageId") 
                        ? ua.get("userMessageId").asText() 
                        : null;
                    
                    IntimacyData intimacy = null;
                    if (ua.has("intimacy")) {
                        JsonNode intimacyNode = ua.get("intimacy");
                        intimacy = new IntimacyData(
                            intimacyNode.get("detectedLevel").asInt(),
                            intimacyNode.get("correctedSentence").asText(),
                            new FeedbackText(
                                intimacyNode.get("feedback").get("ko").asText(),
                                intimacyNode.get("feedback").get("en").asText()
                            ),
                            intimacyNode.get("corrections").asText()
                        );
                    }
                    
                    userAnalysis = new UserMessageAnalysis(userMessageId, intimacy);
                }
                
                // botResponseAnalysis 파싱
                BotResponseAnalysis botAnalysis = null;
                if (root.has("botResponseAnalysis")) {
                    JsonNode ba = root.get("botResponseAnalysis");
                    
                    if (ba.has("vocabulary")) {
                        JsonNode vocabNode = ba.get("vocabulary");
                        List<VocabularyWord> words = new ArrayList<>();
                        
                        if (vocabNode.has("words") && vocabNode.get("words").isArray()) {
                            for (JsonNode wordNode : vocabNode.get("words")) {
                                words.add(new VocabularyWord(
                                    wordNode.get("word").asText(),
                                    wordNode.get("difficulty").asInt(),
                                    new WordContext(
                                        wordNode.get("context").get("roma").asText(),
                                        wordNode.get("context").get("ko").asText(),
                                        wordNode.get("context").get("en").asText()
                                    )
                                ));
                            }
                        }
                        
                        botAnalysis = new BotResponseAnalysis(
                            new VocabularyData(words)
                        );
                    }
                }
                
                metadata = new MessageMetadata(userAnalysis, botAnalysis);
                
            } catch (Exception e) {
                log.error("Metadata 파싱 실패: messageId={}", m.getId(), e);
                metadata = null;
            }
        }
        
        return new MessageResponse(
            m.getId(),
            m.getChatRoom().getId(),
            m.getSenderType(),
            m.getSenderId(),
            m.getContent(),
            m.getContentType(),
            m.getSequenceNumber(),
            m.getIsEdited(),
            m.getIsDeleted(),
            m.getCreatedAt(),
            metadata
        );
    }
}