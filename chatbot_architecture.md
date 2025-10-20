# DoranDoran 챗봇 아키텍처 다이어그램

## Multi-Agent AI 시스템 아키텍처

```mermaid
graph TB
    subgraph "Client Layer"
        WebClient[웹 클라이언트]
        MobileClient[모바일 앱]
    end

    subgraph "API Layer"
        ChatController[Chat Controller]
        SSEController[SSE Controller]
        WebSocketHandler[WebSocket Handler]
    end

    subgraph "Service Layer"
        ChatService[Chat Service]
        MultiAgentOrchestrator[Multi-Agent<br/>Orchestrator]
        PromptService[Prompt Service]
        SSEManager[SSE Manager]
    end

    subgraph "AI Agent Layer"
        IntimacyAgent[Intimacy Agent<br/>친밀도 분석]
        VocabularyAgent[Vocabulary Agent<br/>어휘 추출]
        TranslationAgent[Translation Agent<br/>번역]
        ConversationAgent[Conversation Agent<br/>대화 생성]
    end

    subgraph "External AI Services"
        OpenAI[OpenAI API<br/>GPT Models]
    end

    subgraph "Data Layer"
        ChatDB[(Chat Database<br/>PostgreSQL)]
        Redis[(Redis Cache)]
    end

    subgraph "Integration Layer"
        UserServiceClient[User Service<br/>Feign Client]
        AuthServiceClient[Auth Service<br/>Feign Client]
    end

    %% Client to API
    WebClient --> ChatController
    WebClient --> SSEController
    WebClient --> WebSocketHandler
    MobileClient --> ChatController
    MobileClient --> SSEController

    %% API to Service
    ChatController --> ChatService
    SSEController --> SSEManager
    WebSocketHandler --> ChatService

    %% Service to Orchestrator
    ChatService --> MultiAgentOrchestrator
    MultiAgentOrchestrator --> PromptService

    %% Orchestrator to Agents
    MultiAgentOrchestrator --> IntimacyAgent
    MultiAgentOrchestrator --> VocabularyAgent
    MultiAgentOrchestrator --> TranslationAgent
    MultiAgentOrchestrator --> ConversationAgent

    %% Agents to OpenAI
    IntimacyAgent --> OpenAI
    VocabularyAgent --> OpenAI
    TranslationAgent --> OpenAI
    ConversationAgent --> OpenAI

    %% Service to Data
    ChatService --> ChatDB
    ChatService --> Redis
    MultiAgentOrchestrator --> ChatDB

    %% Service to Integration
    ChatService --> UserServiceClient
    ChatService --> AuthServiceClient

    %% SSE Manager to Client
    SSEManager --> SSEController
```

## Multi-Agent 처리 흐름

### 1. 병렬 처리 단계 (Phase 1)
```mermaid
sequenceDiagram
    participant User
    participant ChatController
    participant MultiAgent
    participant IntimacyAgent
    participant VocabularyAgent
    participant ConversationAgent
    participant OpenAI

    User->>ChatController: 메시지 전송
    ChatController->>MultiAgent: Multi-Agent 처리 시작
    
    par 병렬 실행
        MultiAgent->>IntimacyAgent: 친밀도 분석
        IntimacyAgent->>OpenAI: AI 호출
        OpenAI-->>IntimacyAgent: 분석 결과
        IntimacyAgent-->>MultiAgent: IntimacyAgentResponse
    and
        MultiAgent->>VocabularyAgent: 어휘 추출
        VocabularyAgent->>OpenAI: AI 호출
        OpenAI-->>VocabularyAgent: 추출 결과
        VocabularyAgent-->>MultiAgent: VocabularyAgentResponse
    and
        MultiAgent->>ConversationAgent: 대화 생성
        ConversationAgent->>OpenAI: AI 호출
        OpenAI-->>ConversationAgent: 스트림 응답
        ConversationAgent-->>MultiAgent: Flux<String>
    end
```

### 2. 순차 처리 단계 (Phase 2)
```mermaid
sequenceDiagram
    participant MultiAgent
    participant VocabularyAgent
    participant TranslationAgent
    participant OpenAI

    MultiAgent->>TranslationAgent: VocabularyAgent 결과 전달
    TranslationAgent->>OpenAI: 번역 요청
    OpenAI-->>TranslationAgent: 번역 결과
    TranslationAgent-->>MultiAgent: TranslationAgentResponse
```

### 3. SSE 스트리밍 단계 (Phase 3)
```mermaid
sequenceDiagram
    participant MultiAgent
    participant SSEManager
    participant Client

    MultiAgent->>SSEManager: intimacy_analysis 이벤트
    SSEManager->>Client: SSE 스트림 전송
    
    MultiAgent->>SSEManager: vocabulary_extracted 이벤트
    SSEManager->>Client: SSE 스트림 전송
    
    MultiAgent->>SSEManager: vocabulary_translated 이벤트
    SSEManager->>Client: SSE 스트림 전송
    
    MultiAgent->>SSEManager: conversation_chunk 이벤트들
    SSEManager->>Client: SSE 스트림 전송 (실시간)
    
    MultiAgent->>SSEManager: conversation_complete 이벤트
    SSEManager->>Client: SSE 스트림 전송
    
    MultiAgent->>SSEManager: aggregated_complete 이벤트
    SSEManager->>Client: SSE 스트림 전송
```

## AI Agent 상세 구조

### 1. IntimacyAgent (친밀도 분석)
```mermaid
graph LR
    subgraph "IntimacyAgent"
        Input[사용자 메시지]
        PromptBuilder[프롬프트 빌더]
        OpenAICall[OpenAI 호출]
        ResponseParser[응답 파서]
        Output[IntimacyAgentResponse]
    end

    Input --> PromptBuilder
    PromptBuilder --> OpenAICall
    OpenAICall --> ResponseParser
    ResponseParser --> Output

    subgraph "Response Structure"
        DetectedLevel[detectedLevel: 1-3]
        CorrectedSentence[correctedSentence]
        Feedback[feedback]
        Corrections[corrections: Array]
    end

    Output --> DetectedLevel
    Output --> CorrectedSentence
    Output --> Feedback
    Output --> Corrections
```

### 2. VocabularyAgent (어휘 추출)
```mermaid
graph LR
    subgraph "VocabularyAgent"
        Input[사용자 메시지]
        LevelCheck[사용자 레벨 확인]
        PromptBuilder[프롬프트 빌더]
        OpenAICall[OpenAI 호출]
        ResponseParser[응답 파서]
        Output[VocabularyAgentResponse]
    end

    Input --> LevelCheck
    LevelCheck --> PromptBuilder
    PromptBuilder --> OpenAICall
    OpenAICall --> ResponseParser
    ResponseParser --> Output

    subgraph "Response Structure"
        Words[words: Array]
        Word[word: String]
        Difficulty[difficulty: 1-3]
        Context[context: String]
    end

    Output --> Words
    Words --> Word
    Words --> Difficulty
    Words --> Context
```

### 3. TranslationAgent (번역)
```mermaid
graph LR
    subgraph "TranslationAgent"
        Input[VocabularyAgent 결과]
        PromptBuilder[번역 프롬프트]
        OpenAICall[OpenAI 호출]
        ResponseParser[응답 파서]
        Output[TranslationAgentResponse]
    end

    Input --> PromptBuilder
    PromptBuilder --> OpenAICall
    OpenAICall --> ResponseParser
    ResponseParser --> Output

    subgraph "Response Structure"
        Translations[translations: Array]
        Original[original: String]
        English[english: String]
        Pronunciation[pronunciation: String]
    end

    Output --> Translations
    Translations --> Original
    Translations --> English
    Translations --> Pronunciation
```

### 4. ConversationAgent (대화 생성)
```mermaid
graph LR
    subgraph "ConversationAgent"
        Input[사용자 메시지]
        PromptService[Prompt Service]
        SystemPrompt[시스템 프롬프트]
        OpenAICall[OpenAI 스트림 호출]
        Output[Flux<String>]
    end

    Input --> PromptService
    PromptService --> SystemPrompt
    SystemPrompt --> OpenAICall
    OpenAICall --> Output

    subgraph "Prompt Components"
        ChatbotMeta[챗봇 메타데이터]
        RoomContext[룸 컨텍스트]
        UserPreferences[사용자 선호도]
        SystemDirectives[시스템 지시사항]
    end

    PromptService --> ChatbotMeta
    PromptService --> RoomContext
    PromptService --> UserPreferences
    PromptService --> SystemDirectives
```

## 데이터 모델 구조

### 1. Chatbot 엔티티
```mermaid
classDiagram
    class Chatbot {
        +UUID id
        +String name
        +String displayName
        +String description
        +String botType
        +String modelName
        +JSONB personality
        +String systemPrompt
        +JSONB capabilities
        +JSONB settings
        +Integer intimacyLevel
        +String avatarUrl
        +Boolean isActive
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
        +UUID createdBy
    }

    class Personality {
        +String[] traits
        +SpeakingStyle speakingStyle
        +String[] domainKnowledge
        +Guardrails guardrails
        +String systemPrompt
        +FewShot[] fewShot
        +CustomInstructions customInstructions
    }

    class Capabilities {
        +String model
        +String[] modalities
        +String[] tools
        +Double temperature
        +Double topP
        +Integer maxTokens
        +Safety safety
        +ResponseStyle responseStyle
        +RateLimits rateLimits
    }

    Chatbot --> Personality
    Chatbot --> Capabilities
```

### 2. ChatRoom 엔티티
```mermaid
classDiagram
    class ChatRoom {
        +UUID id
        +String name
        +String description
        +UUID chatbotId
        +UUID userId
        +JSONB settings
        +JSONB contextData
        +LocalDateTime lastMessageAt
        +UUID lastMessageId
        +Boolean isArchived
        +Boolean isDeleted
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class ContextData {
        +String conversationSummary
        +UserPreferences userPreferences
        +SessionData sessionData
        +AIContext aiContext
    }

    class UserPreferences {
        +String responseLength
        +String language
        +String[] topics
    }

    class AIContext {
        +String[] memory
        +ConversationHistory[] conversationHistory
    }

    ChatRoom --> ContextData
    ContextData --> UserPreferences
    ContextData --> AIContext
```

### 3. Message 엔티티
```mermaid
classDiagram
    class Message {
        +UUID id
        +UUID chatroomId
        +String senderType
        +UUID senderId
        +String content
        +String contentType
        +JSONB metadata
        +UUID parentMessageId
        +Long sequenceNumber
        +Integer tokenCount
        +Integer processingTimeMs
        +Boolean isEdited
        +LocalDateTime editedAt
        +Boolean isDeleted
        +LocalDateTime deletedAt
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class MessageMetadata {
        +String codeLanguage
        +String systemMessageType
        +String aiModel
        +Double confidence
        +Integer processingTime
        +TokenUsage tokenUsage
    }

    class TokenUsage {
        +Integer prompt
        +Integer completion
        +Integer total
    }

    Message --> MessageMetadata
    MessageMetadata --> TokenUsage
```

## SSE 이벤트 타입

### 1. 이벤트 흐름
```mermaid
stateDiagram-v2
    [*] --> intimacy_analysis
    intimacy_analysis --> vocabulary_extracted
    vocabulary_extracted --> vocabulary_translated
    vocabulary_translated --> conversation_chunk
    conversation_chunk --> conversation_chunk
    conversation_chunk --> conversation_complete
    conversation_complete --> aggregated_complete
    aggregated_complete --> [*]
```

### 2. 이벤트 데이터 구조
```mermaid
classDiagram
    class SSEEvent {
        +String eventType
        +Object data
        +LocalDateTime timestamp
    }

    class IntimacyAnalysisEvent {
        +Integer detectedLevel
        +String correctedSentence
        +String feedback
        +String[] corrections
    }

    class VocabularyExtractedEvent {
        +VocabularyWord[] words
    }

    class VocabularyTranslatedEvent {
        +TranslatedWord[] translations
    }

    class ConversationChunkEvent {
        +String content
        +Boolean isComplete
        +Integer tokenCount
    }

    class AggregatedCompleteEvent {
        +IntimacyAnalysisEvent intimacy
        +VocabularyExtractedEvent vocabulary
        +VocabularyTranslatedEvent translation
        +String conversationResponse
    }

    SSEEvent --> IntimacyAnalysisEvent
    SSEEvent --> VocabularyExtractedEvent
    SSEEvent --> VocabularyTranslatedEvent
    SSEEvent --> ConversationChunkEvent
    SSEEvent --> AggregatedCompleteEvent
```

## 성능 최적화 전략

### 1. 병렬 처리 최적화
- **IntimacyAgent, VocabularyAgent, ConversationAgent** 동시 실행
- **Reactive Programming** (Mono/Flux) 활용
- **Non-blocking I/O** 처리

### 2. 캐싱 전략
- **Redis**: 채팅방 컨텍스트, 사용자 정보 캐싱
- **Prompt 캐싱**: 자주 사용되는 프롬프트 템플릿
- **AI 응답 캐싱**: 유사한 질문에 대한 응답 재사용

### 3. 스트리밍 최적화
- **SSE**: 실시간 응답 스트리밍
- **WebSocket**: 양방향 실시간 통신
- **백프레셔 제어**: 클라이언트 처리 속도에 맞춘 스트리밍

### 4. 데이터베이스 최적화
- **인덱스**: `messages(chatroom_id, sequence_number)`
- **파티셔닝**: 메시지 테이블 월별 파티셔닝
- **JSONB 인덱스**: GIN 인덱스로 JSON 검색 최적화
