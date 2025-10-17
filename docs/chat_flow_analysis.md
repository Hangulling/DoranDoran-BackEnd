# 채팅 시스템 프론트엔드 전달 흐름 분석

## 전체 아키텍처 개요

현재 도란도란 채팅 시스템은 **Multi-Agent 아키텍처**와 **Server-Sent Events (SSE)**를 활용한 실시간 채팅 시스템입니다.

## 1. 주요 구성 요소

### 백엔드 (Spring Boot)
- **ChatController**: REST API 엔드포인트 제공
- **SSEController**: 실시간 스트림 연결 관리
- **MultiAgentOrchestrator**: 여러 AI 에이전트 조율
- **SSEManager**: SSE 연결 및 이벤트 전송 관리
- **ChatService**: 채팅방 및 메시지 비즈니스 로직

### 프론트엔드 (React + TypeScript)
- **ChatPage**: 메인 채팅 화면
- **ChatBubble**: 메시지 표시 컴포넌트
- **chats.ts**: API 호출 함수들
- **EventSource**: SSE 연결 관리

## 2. 데이터 흐름 상세 분석

### 2.1 메시지 전송 흐름

```
사용자 입력 → ChatFooter → handleSendMessage → API 호출
    ↓
POST /api/chat/chatrooms/{chatroomId}/messages
    ↓
ChatController.sendMessage()
    ↓
ChatService.sendMessage() → DB 저장
    ↓
MultiAgentOrchestrator.processUserMessage()
    ↓
병렬 AI 에이전트 실행:
├── IntimacyAgent (친밀도 분석)
├── VocabularyAgent (어휘 추출)
├── TranslationAgent (번역)
└── ConversationAgent (대화 생성)
    ↓
SSE를 통한 실시간 응답 전송
```

### 2.2 SSE 실시간 통신 흐름

```
프론트엔드: EventSource 연결
    ↓
GET /api/chat/stream/{chatroomId}?userId={userId}
    ↓
SSEController.stream() → SSEManager.create()
    ↓
MultiAgentOrchestrator에서 각 에이전트 완료 시:
├── sseManager.send(chatroomId, "intimacy_analysis", data)
├── sseManager.send(chatroomId, "vocabulary_extracted", data)
├── sseManager.send(chatroomId, "vocabulary_translated", data)
├── sseManager.send(chatroomId, "conversation_chunk", chunk)
└── sseManager.send(chatroomId, "conversation_complete", data)
    ↓
프론트엔드: EventSource 이벤트 수신 및 UI 업데이트
```

## 3. API 엔드포인트 분석

### 3.1 채팅방 관리
- `POST /api/chat/chatrooms` - 채팅방 생성/조회
- `GET /api/chat/chatrooms` - 채팅방 목록 조회
- `GET /api/chat/chatrooms/{chatroomId}` - 특정 채팅방 조회
- `GET /api/chat/chatrooms/{chatroomId}/messages` - 메시지 목록 조회

### 3.2 메시지 처리
- `POST /api/chat/chatrooms/{chatroomId}/messages` - 메시지 전송
- `GET /api/chat/stream/{chatroomId}` - SSE 스트림 연결

### 3.3 챗봇 관리
- `GET /api/chat/chatbots/{chatbotId}` - 챗봇 정보 조회
- `GET /api/chat/chatbots/prompt` - 프롬프트 조회
- `POST /api/chat/chatbots/prompt` - 프롬프트 수정

## 4. Multi-Agent 처리 과정

### 4.1 Phase 1: 병렬 실행
```java
// IntimacyAgent - 친밀도 분석
Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content)

// VocabularyAgent - 어휘 추출
Mono<VocabularyAgentResponse> vocabularyMono = vocabularyAgent.extractDifficultWords(content, userLevel)
```

### 4.2 Phase 2: 의존성 처리
```java
// TranslationAgent - VocabularyAgent 결과에 의존
vocabularyMono.flatMap(vocabResp -> translationAgent.translate(vocabResp.words()))
```

### 4.3 Phase 3: 대화 생성
```java
// ConversationAgent - 독립적 스트림
conversationAgent.generateResponse(chatroomId, content)
    .doOnNext(chunk -> sseManager.send(chatroomId, "conversation_chunk", chunk))
```

### 4.4 Phase 4: 결과 집계
```java
// 모든 에이전트 완료 후 최종 결과 전송
Mono.zip(intimacyMono, vocabularyMono)
    .doOnSuccess(tuple -> sseManager.send(chatroomId, "aggregated_complete", result))
```

## 5. 프론트엔드 이벤트 처리

### 5.1 SSE 이벤트 리스너
```javascript
// conversation_chunk - 실시간 대화 생성
eventSource.addEventListener('conversation_chunk', function(event) {
    const data = event.data;
    conversationBuffer += data; // 청크 누적
});

// conversation_complete - 대화 완료
eventSource.addEventListener('conversation_complete', function(event) {
    const data = JSON.parse(event.data);
    // UI에 완성된 메시지 표시
});

// intimacy_analysis - 친밀도 분석 결과
eventSource.addEventListener('intimacy_analysis', function(event) {
    const data = JSON.parse(event.data);
    // 친밀도 정보 표시
});
```

## 6. 데이터 모델

### 6.1 Message 엔티티
```java
public class Message {
    private UUID id;
    private ChatRoom chatRoom;
    private String senderType; // "user" | "bot"
    private UUID senderId;
    private String content;
    private String contentType;
    private Long sequenceNumber;
    private LocalDateTime createdAt;
}
```

### 6.2 ChatRoom 엔티티
```java
public class ChatRoom {
    private UUID id;
    private User user;
    private Chatbot chatbot;
    private String name;
    private JsonNode settings; // concept, coachmarkShown 등
    private Message lastMessage;
    private LocalDateTime lastMessageAt;
}
```

## 7. 특징 및 장점

### 7.1 실시간성
- SSE를 통한 실시간 응답 전송
- 스트리밍 방식의 대화 생성 (conversation_chunk)
- 병렬 처리로 빠른 응답

### 7.2 확장성
- Multi-Agent 아키텍처로 기능 확장 용이
- 각 에이전트는 독립적으로 동작
- Reactive Programming (Mono/Flux) 활용

### 7.3 사용자 경험
- 점진적 메시지 표시 (타이핑 효과)
- 친밀도 기반 맞춤형 응답
- 어휘 학습 및 번역 기능

## 8. 개선 가능한 부분

### 8.1 프론트엔드
- 현재 Mock 데이터 사용 중 (실제 API 연동 필요)
- SSE 연결 상태 관리 개선
- 에러 처리 강화

### 8.2 백엔드
- SSE 연결 풀 관리 최적화
- 에이전트 실패 시 복구 로직
- 메시지 큐잉 시스템 도입 고려

이러한 구조로 도란도란 채팅 시스템은 사용자에게 실시간이고 개인화된 언어 학습 경험을 제공하고 있습니다.
