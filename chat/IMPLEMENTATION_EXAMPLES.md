# 챗봇 고도화 구현 예시 코드

## 1. LangChain4j 통합 예시

### 1.1 build.gradle 의존성 추가
```gradle
dependencies {
    // LangChain4j
    implementation 'dev.langchain4j:langchain4j:0.30.0'
    implementation 'dev.langchain4j:langchain4j-open-ai:0.30.0'
    implementation 'dev.langchain4j:langchain4j-spring-boot-starter:0.30.0'
    
    // Mustache 템플릿 (프롬프트 템플릿용)
    implementation 'com.github.spullara.mustache.java:compiler:0.9.10'
}
```

### 1.2 application.yml 설정
```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-4o-mini
      temperature: 0.85
      max-tokens: 800
      timeout: 60s
    embedding-model:
      api-key: ${OPENAI_API_KEY}
      model-name: text-embedding-3-small
      dimensions: 1536
```

### 1.3 LangChain4j 기반 ConversationAgent
```java
package com.dorandoran.chat.service.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.streaming.StreamingChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class LangChainConversationAgent {
    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final PromptTemplateService promptTemplateService;
    
    // 채팅방별 메모리 관리
    private final ConcurrentMap<UUID, ChatMemory> chatMemories = new ConcurrentHashMap<>();
    
    public Flux<String> generateResponse(UUID chatroomId, String userMessage) {
        log.info("=== LangChainConversationAgent.generateResponse() 호출됨 ===");
        
        // 1. 메모리 로드 또는 생성
        ChatMemory memory = chatMemories.computeIfAbsent(
            chatroomId, 
            k -> MessageWindowChatMemory.withMaxMessages(10)
        );
        
        // 2. 시스템 프롬프트 로드
        String systemPrompt = promptTemplateService.buildSystemPrompt(chatroomId);
        
        // 3. 사용자 메시지 추가
        memory.add(UserMessage.from(userMessage));
        
        // 4. 스트리밍 응답 생성
        return Flux.create(sink -> {
            try {
                streamingChatModel.generate(
                    memory.messages(),
                    systemPrompt
                ).onNext(token -> {
                    sink.next(token);
                }).onComplete(() -> {
                    sink.complete();
                }).onError(error -> {
                    log.error("LangChain 스트리밍 오류", error);
                    sink.error(error);
                }).start();
            } catch (Exception e) {
                log.error("LangChain 응답 생성 실패", e);
                sink.error(e);
            }
        })
        .doOnComplete(() -> {
            // 5. 응답 완료 후 메모리에 저장
            // (실제로는 스트림 완료 후 전체 응답을 메모리에 추가해야 함)
            log.info("LangChain 응답 완료");
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
}
```

### 1.4 프롬프트 템플릿 서비스
```java
package com.dorandoran.chat.service;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptTemplateService {
    private final ResourceLoader resourceLoader;
    private final MustacheFactory mustacheFactory;
    private final ChatService chatService;
    
    public String buildSystemPrompt(UUID chatroomId) {
        try {
            // 1. 템플릿 파일 로드
            Resource templateResource = resourceLoader.getResource(
                "classpath:prompts/conversation/system_prompt.mustache"
            );
            String templateContent = new String(
                templateResource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
            );
            
            // 2. Mustache 템플릿 컴파일
            Mustache mustache = mustacheFactory.compile(
                new StringReader(templateContent),
                "system_prompt"
            );
            
            // 3. 변수 준비
            Map<String, Object> variables = Map.of(
                "concept", chatService.getConcept(chatroomId),
                "intimacyLevel", chatService.getIntimacyLevel(chatroomId),
                "guidelines", getGuidelines(chatroomId)
            );
            
            // 4. 템플릿 렌더링
            StringWriter writer = new StringWriter();
            mustache.execute(writer, variables);
            
            return writer.toString();
        } catch (Exception e) {
            log.error("프롬프트 템플릿 로드 실패", e);
            return getDefaultPrompt(chatroomId);
        }
    }
    
    private String getGuidelines(UUID chatroomId) {
        String concept = chatService.getConcept(chatroomId);
        int level = chatService.getIntimacyLevel(chatroomId);
        
        // 컨셉별 가이드라인 반환
        return switch (concept) {
            case "FRIEND" -> getFriendGuidelines(level);
            case "COWORKER" -> getCoworkerGuidelines(level);
            default -> getDefaultGuidelines();
        };
    }
    
    private String getFriendGuidelines(int level) {
        return switch (level) {
            case 1 -> "반말 사용, 존댓말 금지";
            case 3 -> "반말 + 줄임말 허용 (ㅇㅇ, ㄱㄱ)";
            default -> "반말 사용";
        };
    }
    
    private String getCoworkerGuidelines(int level) {
        return switch (level) {
            case 1 -> "존댓말 사용, 격식 표현";
            case 3 -> "존댓말 + 정중한 표현";
            default -> "존댓말 사용";
        };
    }
    
    private String getDefaultGuidelines() {
        return "자연스러운 대화";
    }
    
    private String getDefaultPrompt(UUID chatroomId) {
        // 폴백: 기존 하드코딩된 프롬프트
        return "너는 친절한 한국어 챗봇이야.";
    }
}
```

### 1.5 프롬프트 템플릿 파일 예시
```mustache
{{! prompts/conversation/system_prompt.mustache }}
너는 AI 챗봇으로서 사용자와 자연스러운 한국어 대화를 나누는 역할을 수행할거야.

**컨셉**: {{concept}}
**친밀도 레벨**: {{intimacyLevel}}

**말투 가이드라인**:
{{guidelines}}

**주의사항**:
- 컨셉과 친밀도 레벨에 맞는 말투를 사용할 것
- 자연스럽고 일관된 대화를 유지할 것
- 사용자의 감정과 맥락을 고려할 것
```

---

## 2. RAG 구현 예시

### 2.1 PostgreSQL + pgvector 설정

#### 2.1.1 pgvector 확장 설치
```sql
-- PostgreSQL에서 실행
CREATE EXTENSION IF NOT EXISTS vector;
```

#### 2.1.2 임베딩 테이블 생성
```sql
CREATE TABLE chat_schema.message_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID REFERENCES chat_schema.messages(id) ON DELETE CASCADE,
    chatroom_id UUID REFERENCES chat_schema.chatrooms(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL, -- OpenAI text-embedding-3-small
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 벡터 검색 인덱스 (HNSW 알고리즘)
CREATE INDEX ON chat_schema.message_embeddings 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- chatroom_id 인덱스
CREATE INDEX idx_message_embeddings_chatroom 
ON chat_schema.message_embeddings(chatroom_id);

-- message_id 인덱스
CREATE INDEX idx_message_embeddings_message 
ON chat_schema.message_embeddings(message_id);
```

### 2.2 Entity 클래스
```java
package com.dorandoran.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_embeddings", schema = "chat_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEmbedding {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "message_id", nullable = false)
    private UUID messageId;
    
    @Column(name = "chatroom_id", nullable = false)
    private UUID chatroomId;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    private String embedding; // PostgreSQL vector 타입은 String으로 저장
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
```

### 2.3 Repository (벡터 검색)
```java
package com.dorandoran.chat.repository;

import com.dorandoran.chat.entity.MessageEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageEmbeddingRepository extends JpaRepository<MessageEmbedding, UUID> {
    
    /**
     * 코사인 유사도 기반 벡터 검색
     * @param chatroomId 채팅방 ID
     * @param queryEmbedding 검색할 임베딩 벡터 (문자열 형식: "[0.1, 0.2, ...]")
     * @param similarityThreshold 유사도 임계값 (0.0 ~ 1.0)
     * @param limit 결과 개수
     * @return 유사한 메시지 임베딩 목록
     */
    @Query(value = """
        SELECT * FROM chat_schema.message_embeddings
        WHERE chatroom_id = :chatroomId
        AND 1 - (embedding <=> :queryEmbedding::vector) >= :threshold
        ORDER BY embedding <=> :queryEmbedding::vector
        LIMIT :limit
        """, nativeQuery = true)
    List<MessageEmbedding> findSimilarEmbeddings(
        @Param("chatroomId") UUID chatroomId,
        @Param("queryEmbedding") String queryEmbedding,
        @Param("threshold") double similarityThreshold,
        @Param("limit") int limit
    );
}
```

### 2.4 EmbeddingService
```java
package com.dorandoran.chat.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;
    
    /**
     * 텍스트를 임베딩 벡터로 변환
     * @param text 임베딩할 텍스트
     * @return PostgreSQL vector 형식 문자열: "[0.1, 0.2, ...]"
     */
    public Mono<String> generateEmbedding(String text) {
        return Mono.fromCallable(() -> {
            Embedding embedding = embeddingModel.embed(text).content();
            float[] vector = embedding.vectorAsFloatArray();
            
            // PostgreSQL vector 형식으로 변환
            String vectorString = Arrays.stream(vector)
                .mapToObj(f -> String.valueOf(f))
                .collect(Collectors.joining(", ", "[", "]"));
            
            log.debug("임베딩 생성 완료: text='{}', vectorLength={}", 
                text.substring(0, Math.min(50, text.length())), 
                vector.length);
            
            return vectorString;
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * float 배열을 PostgreSQL vector 형식으로 변환
     */
    public String floatArrayToVectorString(float[] vector) {
        return Arrays.stream(vector)
            .mapToObj(f -> String.valueOf(f))
            .collect(Collectors.joining(", ", "[", "]"));
    }
}
```

### 2.5 RAG 기반 VocabularyAgent
```java
package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.entity.MessageEmbedding;
import com.dorandoran.chat.repository.MessageEmbeddingRepository;
import com.dorandoran.chat.service.EmbeddingService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGVocabularyAgent {
    private final EmbeddingService embeddingService;
    private final MessageEmbeddingRepository embeddingRepository;
    private final ChatLanguageModel chatModel;
    
    /**
     * RAG를 활용한 어휘 설명 생성
     * 1. 단어 임베딩 생성
     * 2. 유사한 설명 검색
     * 3. 검색된 컨텍스트를 활용하여 설명 생성
     */
    public Mono<VocabularyExplanation> explainWord(
        String word, 
        UUID chatroomId,
        String concept,
        int intimacyLevel
    ) {
        log.info("=== RAGVocabularyAgent.explainWord() 호출 ===");
        log.info("word='{}', chatroomId={}", word, chatroomId);
        
        // 1. 단어 임베딩 생성
        return embeddingService.generateEmbedding(word)
            .flatMap(wordEmbedding -> {
                log.info("임베딩 생성 완료: word='{}'", word);
                
                // 2. 유사한 설명 검색 (같은 chatroomId 내)
                List<MessageEmbedding> similar = embeddingRepository
                    .findSimilarEmbeddings(
                        chatroomId,
                        wordEmbedding,
                        0.7, // 유사도 임계값
                        5    // 상위 5개
                    );
                
                log.info("유사한 설명 {}개 검색됨", similar.size());
                
                // 3. 검색된 컨텍스트를 프롬프트에 주입
                String context = similar.stream()
                    .map(MessageEmbedding::getContent)
                    .collect(Collectors.joining("\n---\n"));
                
                String prompt = buildPromptWithContext(word, context, concept, intimacyLevel);
                
                log.info("프롬프트 생성 완료 (길이: {})", prompt.length());
                
                // 4. LLM으로 설명 생성
                return Mono.fromCallable(() -> {
                    String explanation = chatModel.generate(prompt);
                    return parseExplanation(explanation);
                })
                .subscribeOn(Schedulers.boundedElastic());
            })
            .doOnError(error -> log.error("RAG 어휘 설명 생성 실패", error));
    }
    
    private String buildPromptWithContext(
        String word, 
        String context, 
        String concept,
        int intimacyLevel
    ) {
        if (context.isEmpty()) {
            // 컨텍스트가 없으면 기본 프롬프트
            return String.format("""
                다음 단어를 설명하세요: %s
                컨셉: %s, 친밀도 레벨: %d
                """, word, concept, intimacyLevel);
        }
        
        return String.format("""
            다음 단어를 설명하세요: %s
            컨셉: %s, 친밀도 레벨: %d
            
            참고할 이전 설명들:
            %s
            
            위 설명들을 참고하되, 새로운 관점도 추가하세요.
            일관성 있으면서도 다양성을 유지하세요.
            """, word, concept, intimacyLevel, context);
    }
    
    private VocabularyExplanation parseExplanation(String explanation) {
        // JSON 파싱 또는 구조화된 응답 파싱
        // 실제 구현은 응답 형식에 따라 다름
        return new VocabularyExplanation(explanation);
    }
    
    public record VocabularyExplanation(String explanation) {}
}
```

### 2.6 메시지 저장 시 임베딩 자동 생성
```java
package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.entity.MessageEmbedding;
import com.dorandoran.chat.repository.MessageEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageEmbeddingService {
    private final EmbeddingService embeddingService;
    private final MessageEmbeddingRepository embeddingRepository;
    
    /**
     * 메시지 저장 시 임베딩 자동 생성 (비동기)
     */
    @Async
    public void createEmbeddingAsync(Message message) {
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            return;
        }
        
        embeddingService.generateEmbedding(message.getContent())
            .subscribe(
                embedding -> {
                    MessageEmbedding messageEmbedding = MessageEmbedding.builder()
                        .messageId(message.getId())
                        .chatroomId(message.getChatRoom().getId())
                        .content(message.getContent())
                        .embedding(embedding)
                        .metadata("{\"senderType\":\"" + message.getSenderType() + "\"}")
                        .createdAt(LocalDateTime.now())
                        .build();
                    
                    embeddingRepository.save(messageEmbedding);
                    log.info("메시지 임베딩 저장 완료: messageId={}", message.getId());
                },
                error -> log.error("메시지 임베딩 생성 실패: messageId={}", message.getId(), error)
            );
    }
}
```

---

## 3. 하이브리드 메모리 관리

### 3.1 HybridMemoryManager
```java
package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.repository.MessageRepository;
import com.dorandoran.chat.service.agent.SummarizerAgent;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridMemoryManager {
    private final MessageRepository messageRepository;
    private final SummarizerAgent summarizerAgent;
    private final ContextRetrievalService contextRetrieval;
    
    @Data
    @Builder
    public static class ConversationContext {
        private List<Message> shortTermMemory;      // 최근 5개 메시지
        private String longTermSummary;              // 요약
        private List<String> relevantContext;        // RAG 검색 결과
    }
    
    /**
     * 하이브리드 컨텍스트 구축
     * 1. 단기 메모리: 최근 메시지
     * 2. 장기 메모리: 요약
     * 3. 관련 컨텍스트: RAG 검색
     */
    public Mono<ConversationContext> buildContext(UUID chatroomId, String currentMessage) {
        log.info("=== HybridMemoryManager.buildContext() 호출 ===");
        
        // 1. 단기 메모리: 최근 5개 메시지
        List<Message> shortTerm = messageRepository
            .findRecentMessages(chatroomId, 5);
        
        log.info("단기 메모리: {}개 메시지", shortTerm.size());
        
        // 2. 장기 메모리: 요약
        Mono<String> summaryMono = Mono.fromCallable(() -> 
            summarizerAgent.getLatestSummary(chatroomId)
        ).subscribeOn(Schedulers.boundedElastic());
        
        // 3. 관련 컨텍스트 검색 (RAG)
        Mono<List<String>> contextMono = contextRetrieval
            .retrieveRelevantContext(chatroomId, currentMessage, 3);
        
        // 병렬 실행 후 조합
        return Mono.zip(summaryMono, contextMono)
            .map(tuple -> {
                String summary = tuple.getT1();
                List<String> relevantContext = tuple.getT2();
                
                log.info("장기 메모리: 요약 길이={}, 관련 컨텍스트={}개", 
                    summary != null ? summary.length() : 0, 
                    relevantContext.size());
                
                return ConversationContext.builder()
                    .shortTermMemory(shortTerm)
                    .longTermSummary(summary)
                    .relevantContext(relevantContext)
                    .build();
            });
    }
    
    /**
     * 컨텍스트를 프롬프트에 주입
     */
    public String injectContext(String basePrompt, ConversationContext context) {
        StringBuilder prompt = new StringBuilder(basePrompt);
        
        // 장기 메모리 (요약)
        if (context.getLongTermSummary() != null && !context.getLongTermSummary().isEmpty()) {
            prompt.append("\n\n**이전 대화 요약:**\n");
            prompt.append(context.getLongTermSummary());
        }
        
        // 관련 컨텍스트
        if (!context.getRelevantContext().isEmpty()) {
            prompt.append("\n\n**관련 대화 맥락:**\n");
            for (String ctx : context.getRelevantContext()) {
                prompt.append("- ").append(ctx).append("\n");
            }
        }
        
        // 단기 메모리 (최근 메시지)
        if (!context.getShortTermMemory().isEmpty()) {
            prompt.append("\n\n**최근 대화:**\n");
            for (Message msg : context.getShortTermMemory()) {
                prompt.append(String.format("%s: %s\n", 
                    msg.getSenderType(), 
                    msg.getContent()));
            }
        }
        
        return prompt.toString();
    }
}
```

---

## 4. 통합 예시: LangChain4j + RAG

### 4.1 통합 ConversationAgent
```java
package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.service.HybridMemoryManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegratedConversationAgent {
    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final HybridMemoryManager memoryManager;
    private final PromptTemplateService promptTemplateService;
    
    private final ConcurrentMap<UUID, ChatMemory> chatMemories = new ConcurrentHashMap<>();
    
    public Flux<String> generateResponse(UUID chatroomId, String userMessage) {
        log.info("=== IntegratedConversationAgent.generateResponse() 호출 ===");
        
        // 1. 하이브리드 컨텍스트 구축
        return memoryManager.buildContext(chatroomId, userMessage)
            .flatMapMany(context -> {
                // 2. 메모리 로드
                ChatMemory memory = chatMemories.computeIfAbsent(
                    chatroomId,
                    k -> MessageWindowChatMemory.withMaxMessages(10)
                );
                
                // 3. 시스템 프롬프트 + 컨텍스트 주입
                String basePrompt = promptTemplateService.buildSystemPrompt(chatroomId);
                String enhancedPrompt = memoryManager.injectContext(basePrompt, context);
                
                // 4. 사용자 메시지 추가
                memory.add(UserMessage.from(userMessage));
                
                // 5. 스트리밍 응답 생성
                return Flux.create(sink -> {
                    try {
                        streamingChatModel.generate(
                            memory.messages(),
                            enhancedPrompt
                        ).onNext(token -> {
                            sink.next(token);
                        }).onComplete(() -> {
                            sink.complete();
                        }).onError(error -> {
                            log.error("스트리밍 오류", error);
                            sink.error(error);
                        }).start();
                    } catch (Exception e) {
                        log.error("응답 생성 실패", e);
                        sink.error(e);
                    }
                });
            })
            .subscribeOn(Schedulers.boundedElastic());
    }
}
```

---

## 5. 마이그레이션 체크리스트

### Phase 1: LangChain4j 도입
- [ ] build.gradle에 LangChain4j 의존성 추가
- [ ] application.yml 설정 추가
- [ ] PromptTemplateService 구현
- [ ] 프롬프트 템플릿 파일 생성 (resources/prompts/)
- [ ] LangChainConversationAgent 구현
- [ ] 기존 ConversationAgent와 병행 운영
- [ ] 테스트 및 검증

### Phase 2: RAG 도입
- [ ] PostgreSQL에 pgvector 확장 설치
- [ ] message_embeddings 테이블 생성
- [ ] MessageEmbedding 엔티티 생성
- [ ] MessageEmbeddingRepository 구현
- [ ] EmbeddingService 구현
- [ ] RAGVocabularyAgent 구현
- [ ] MessageEmbeddingService 구현 (자동 임베딩)
- [ ] 기존 메시지 임베딩 생성 배치 작업

### Phase 3: 하이브리드 메모리
- [ ] HybridMemoryManager 구현
- [ ] ContextRetrievalService 구현
- [ ] IntegratedConversationAgent 구현
- [ ] 토큰 윈도우 관리 로직 추가
- [ ] 테스트 및 성능 측정

### Phase 4: 통합 및 최적화
- [ ] 모든 Agent 통합
- [ ] 성능 최적화 (캐싱, 배치 처리)
- [ ] 모니터링 및 로깅 강화
- [ ] 문서화


