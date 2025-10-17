# DoranDoran SSE (Server-Sent Events) 구현 가이드

## 📋 개요

DoranDoran 프로젝트에서 Multi-Agent AI 시스템의 실시간 응답을 위해 Server-Sent Events (SSE)를 구현했습니다. 이 문서는 백엔드와 프론트엔드의 SSE 구현 방식을 상세히 설명합니다.

## 🏗️ 아키텍처 개요

```
사용자 메시지 → Multi-Agent 처리 → SSE 이벤트 전송 → 프론트엔드 실시간 표시
```

## 🔧 백엔드 구현

### 1. SSE 컨트롤러 (SSEController.java)

**역할**: SSE 연결의 진입점, 인증 및 권한 확인

```java
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class SSEController {

    private final SSEManager sseManager;
    private final ChatRoomRepository chatRoomRepository;

    @GetMapping(value = "/stream/{chatroomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@PathVariable UUID chatroomId, 
                                            @RequestParam(required = false) UUID userId) {
        // 1. 사용자 인증 확인
        UUID uid = extractUserIdFromSecurityContext();
        if (uid == null && userId != null) {
            uid = userId;
        }
        if (uid == null) {
            log.warn("SSE 연결 실패: 사용자 ID 없음, chatroomId={}", chatroomId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        
        // 2. 채팅방 접근 권한 확인
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(uid, chatroomId)) {
            log.warn("SSE 접근 거부: userId={}, chatroomId={}", uid, chatroomId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // 3. SSE 연결 생성
        log.info("SSE 연결 성공: userId={}, chatroomId={}", uid, chatroomId);
        return ResponseEntity.ok(sseManager.create(chatroomId));
    }
}
```

### 2. SSE 매니저 (SSEManager.java)

**역할**: SSE 연결 관리, 이벤트 전송

```java
@Component
public class SSEManager {

    // 채팅방별로 여러 연결을 관리
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // SSE 연결 생성
    public SseEmitter create(UUID chatroomId) {
        SseEmitter emitter = new SseEmitter(0L); // 무제한 타임아웃
        
        // 연결 종료 시 자동 정리
        emitter.onCompletion(() -> remove(chatroomId, emitter));
        emitter.onTimeout(() -> remove(chatroomId, emitter));
        emitter.onError((ex) -> remove(chatroomId, emitter));
        
        emitters.computeIfAbsent(chatroomId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        return emitter;
    }

    // 이벤트 전송
    public void send(UUID chatroomId, String eventName, Object data) {
        List<SseEmitter> list = emitters.get(chatroomId);
        if (list == null || list.isEmpty()) return;
        
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventName)  // 이벤트 타입
                    .data(data, MediaType.APPLICATION_JSON)); // 데이터
            } catch (IOException e) {
                remove(chatroomId, emitter);
            }
        }
    }

    // 연결 제거
    private void remove(UUID chatroomId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(chatroomId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(chatroomId);
            }
        }
    }
}
```

### 3. Multi-Agent에서 SSE 사용

**역할**: 각 Agent의 처리 결과를 실시간으로 전송

```java
@Service
@RequiredArgsConstructor
public class MultiAgentOrchestrator {

    private final SSEManager sseManager;
    private final IntimacyAgent intimacyAgent;
    private final VocabularyAgent vocabularyAgent;
    private final TranslationAgent translationAgent;
    private final ConversationAgent conversationAgent;

    public void processUserMessage(UUID chatroomId, UUID userId, String content) {
        
        // Phase 1: IntimacyAgent (친밀도 분석)
        intimacyAgent.analyze(chatroomId, content)
            .doOnNext(resp -> {
                sseManager.send(chatroomId, "intimacy_analysis", Map.of(
                    "detectedLevel", resp.detectedLevel(),
                    "correctedSentence", resp.correctedSentence(),
                    "feedback", resp.feedback(),
                    "corrections", resp.corrections()
                ));
            });

        // Phase 2: VocabularyAgent (어휘 추출)
        vocabularyAgent.extractDifficultWords(content, userLevel)
            .doOnNext(resp -> {
                sseManager.send(chatroomId, "vocabulary_extracted", Map.of(
                    "words", resp.words()
                ));
            });

        // Phase 3: TranslationAgent (번역)
        translationAgent.translate(vocabResp.words())
            .doOnNext(resp -> {
                sseManager.send(chatroomId, "vocabulary_translated", Map.of(
                    "translations", resp.translations()
                ));
            });

        // Phase 4: ConversationAgent (대화 생성) - 스트리밍
        conversationAgent.generateResponse(chatroomId, content)
            .doOnNext(chunk -> {
                // 순수 텍스트로 청크 전송
                sseManager.send(chatroomId, "conversation_chunk", chunk);
            })
            .collectList()
            .doOnSuccess(chunks -> {
                String fullResponse = String.join("", chunks);
                sseManager.send(chatroomId, "conversation_complete", Map.of(
                    "messageId", botMessage.getId(),
                    "content", fullResponse
                ));
            });

        // Phase 5: 전체 결과 집계
        Mono.zip(intimacyMono, vocabularyMono)
            .doOnSuccess(tuple -> {
                sseManager.send(chatroomId, "aggregated_complete", aggregatedResult);
            });
    }
}
```

## 🌐 프론트엔드 구현

### 1. SSE 연결 생성

```javascript
let eventSource = null;

function connectSSE() {
    const chatroomId = document.getElementById('streamChatroomId').value;
    const userId = document.getElementById('userId').value;
    
    // 기존 연결이 있으면 끊기
    if (eventSource) {
        eventSource.close();
    }
    
    // 새로운 SSE 연결 생성
    eventSource = new EventSource(`${BASE_URL}/api/chat/stream/${chatroomId}?userId=${userId}`);
    
    // 연결 성공 시
    eventSource.onopen = function() {
        showStatus('sseStatus', 'success', 'SSE 연결됨! 실시간 응답을 기다리는 중...');
        addSSELog('🔗 SSE 연결 성공');
    };
    
    // 연결 오류 시
    eventSource.onerror = function(event) {
        showStatus('sseStatus', 'error', 'SSE 연결 오류');
        addSSELog('❌ SSE 연결 오류');
    };
}
```

### 2. 이벤트 리스너 등록

```javascript
// Multi-Agent 이벤트 타입들 처리
eventSource.addEventListener('intimacy_analysis', function(event) {
    const data = JSON.parse(event.data);
    addSSELog('🎯 IntimacyAgent: ' + JSON.stringify(data, null, 2));
    // UI에 친밀도 분석 결과 표시
});

eventSource.addEventListener('vocabulary_extracted', function(event) {
    const data = JSON.parse(event.data);
    addSSELog('📚 VocabularyAgent: ' + JSON.stringify(data, null, 2));
    // UI에 어려운 단어 표시
});

eventSource.addEventListener('vocabulary_translated', function(event) {
    const data = JSON.parse(event.data);
    addSSELog('🌐 TranslationAgent: ' + JSON.stringify(data, null, 2));
    // UI에 번역 결과 표시
});

eventSource.addEventListener('conversation_chunk', function(event) {
    addSSELog('💬 ConversationAgent (스트림): ' + event.data);
    // 실시간으로 AI 응답 조각 표시
});

eventSource.addEventListener('conversation_complete', function(event) {
    const data = JSON.parse(event.data);
    addSSELog('✅ ConversationAgent 완료: ' + JSON.stringify(data, null, 2));
    // 완성된 AI 응답 표시
});

eventSource.addEventListener('aggregated_complete', function(event) {
    const data = JSON.parse(event.data);
    addSSELog('🎉 전체 결과 집계: ' + JSON.stringify(data, null, 2));
    // 모든 처리 완료 표시
});
```

### 3. 고급 UI 처리 (test-ai-backend.html)

```javascript
let conversationBuffer = '';  // 대화 청크 버퍼
let pendingResponses = {      // 대기 중인 응답들
    intimacy: null,
    vocabulary: null,
    translation: null,
    conversation: null
};

// 대화 청크 처리 (실시간 스트리밍)
eventSource.addEventListener('conversation_chunk', function(event) {
    try {
        const data = event.data;
        log(`대화 청크 수신: ${data}`, 'info');
        
        // 청크를 버퍼에 저장만 하고 표시하지 않음
        conversationBuffer += data;
    } catch (e) {
        log(`대화 청크 처리 오류: ${e.message}`, 'error');
    }
});

// 대화 완료 시 UI에 표시
eventSource.addEventListener('conversation_complete', function(event) {
    try {
        const data = JSON.parse(event.data);
        log(`대화 완료: ${data.content}`, 'success');
        
        // 완성된 응답을 UI에 표시
        pendingResponses.conversation = data.content;
        checkAndDisplayAllResponses();
    } catch (e) {
        log(`대화 완료 파싱 오류: ${e.message}`, 'error');
    }
});

// 모든 응답이 준비되면 UI에 표시
function checkAndDisplayAllResponses() {
    if (pendingResponses.conversation && 
        pendingResponses.intimacy && 
        pendingResponses.vocabulary && 
        pendingResponses.translation) {
        
        // 채팅 UI에 메시지 추가
        addMessageToChat(pendingResponses.conversation, 'bot');
        
        // 친밀도 피드백 표시
        if (pendingResponses.intimacy.feedback) {
            addMessageToChat(`💡 친밀도 피드백: ${pendingResponses.intimacy.feedback}`, 'system');
        }
        
        // 어휘 학습 표시
        if (pendingResponses.vocabulary.word) {
            addMessageToChat(`📚 어려운 단어: ${pendingResponses.vocabulary.word}`, 'system');
        }
        
        // 번역 표시
        if (pendingResponses.translation.text) {
            addMessageToChat(`🌐 번역: ${pendingResponses.translation.text}`, 'system');
        }
        
        // 응답 초기화
        pendingResponses = {
            intimacy: null,
            vocabulary: null,
            translation: null,
            conversation: null
        };
    }
}
```

### 4. 연결 해제 처리

```javascript
// SSE 연결 해제
function disconnectSSE() {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
        isConnected = false;
        updateSSEStatus(false);
        log('SSE 연결 끊김', 'info');
    }
}

// 페이지를 떠날 때 자동으로 연결 해제
window.addEventListener('beforeunload', function() {
    if (eventSource) {
        eventSource.close();
    }
});
```

## 📊 SSE 이벤트 타입 명세

### 이벤트 타입 목록

| 이벤트 타입 | 데이터 형식 | 설명 | 전송 시점 |
|------------|------------|------|----------|
| `intimacy_analysis` | JSON | 친밀도 분석 결과 | IntimacyAgent 완료 시 |
| `vocabulary_extracted` | JSON | 어려운 단어 추출 결과 | VocabularyAgent 완료 시 |
| `vocabulary_translated` | JSON | 번역 결과 | TranslationAgent 완료 시 |
| `conversation_chunk` | Text | AI 응답 청크 (스트리밍) | ConversationAgent 처리 중 |
| `conversation_complete` | JSON | AI 응답 완료 | ConversationAgent 완료 시 |
| `aggregated_complete` | JSON | 전체 결과 집계 | 모든 Agent 완료 시 |
| `ai_info` | Text | AI 정보 메시지 | 필요 시 |
| `ai_error` | Text | AI 오류 메시지 | 오류 발생 시 |
| `conversation_error` | JSON | 대화 생성 오류 | ConversationAgent 오류 시 |
| `agent_error` | JSON | 에이전트 오류 | Agent 처리 오류 시 |

### 이벤트 데이터 구조

#### 1. intimacy_analysis
```json
{
  "detectedLevel": 2,
  "correctedSentence": "안녕하세요!",
  "feedback": "좋은 존댓말이에요!",
  "corrections": ["격식체 사용"]
}
```

#### 2. vocabulary_extracted
```json
{
  "words": [
    {
      "word": "배우고",
      "difficulty": 2,
      "context": "학습"
    }
  ]
}
```

#### 3. vocabulary_translated
```json
{
  "translations": [
    {
      "original": "배우고",
      "english": "learning",
      "pronunciation": "[배우고]"
    }
  ]
}
```

#### 4. conversation_chunk
```
"안녕하세요! 한국어를"
"배우는 것을 도와드릴게요!"
```

#### 5. conversation_complete
```json
{
  "messageId": "uuid-123",
  "content": "안녕하세요! 한국어를 배우는 것을 도와드릴게요!"
}
```

#### 6. aggregated_complete
```json
{
  "intimacy": {
    "detectedLevel": 2,
    "feedback": "좋아요!"
  },
  "vocabulary": {
    "words": 1
  }
}
```

## 🔄 전체 처리 흐름

```
사용자 메시지 전송
        ↓
   MultiAgentOrchestrator
        ↓
    ┌─────────────────┐
    │  IntimacyAgent  │ → SSE: intimacy_analysis
    └─────────────────┘
        ↓
    ┌─────────────────┐
    │ VocabularyAgent │ → SSE: vocabulary_extracted
    └─────────────────┘
        ↓
    ┌─────────────────┐
    │TranslationAgent │ → SSE: vocabulary_translated
    └─────────────────┘
        ↓
    ┌─────────────────┐
    │ConversationAgent│ → SSE: conversation_chunk (스트리밍)
    └─────────────────┘
        ↓
    ┌─────────────────┐
    │   완료 처리     │ → SSE: conversation_complete
    └─────────────────┘
        ↓
    ┌─────────────────┐
    │   결과 집계     │ → SSE: aggregated_complete
    └─────────────────┘
```

## 🎯 핵심 특징

### 백엔드
- **비동기 처리**: 모든 Agent가 병렬로 동작
- **실시간 피드백**: 각 단계마다 즉시 클라이언트에 알림
- **연결 관리**: 채팅방별로 독립적인 연결 관리
- **에러 처리**: 연결 끊김, 타임아웃 자동 처리

### 프론트엔드
- **실시간 업데이트**: 각 이벤트마다 즉시 UI 반영
- **버퍼링**: 대화 청크를 모아서 완성된 메시지로 표시
- **에러 처리**: JSON 파싱 오류, 연결 오류 등 처리
- **사용자 경험**: 로딩 상태, 연결 상태 표시

## 🚀 사용 방법

### 1. 백엔드에서 이벤트 전송
```java
sseManager.send(chatroomId, "event_name", data);
```

### 2. 프론트엔드에서 이벤트 수신
```javascript
eventSource.addEventListener('event_name', function(event) {
    const data = JSON.parse(event.data);
    // 처리 로직
});
```

### 3. 연결 관리
```javascript
// 연결
const eventSource = new EventSource('/api/chat/stream/chatroomId');

// 해제
eventSource.close();
```

## 📝 주의사항

1. **JSON 파싱**: 대부분의 이벤트는 JSON 데이터이므로 `JSON.parse()` 필요
2. **연결 해제**: 페이지 이탈 시 반드시 연결 해제
3. **에러 처리**: 네트워크 오류, JSON 파싱 오류 등 처리 필요
4. **브라우저 지원**: EventSource API는 대부분의 모던 브라우저에서 지원

## 🔧 테스트 방법

1. **test-ai.html**: 기본 SSE 연결 및 이벤트 수신 테스트
2. **test-ai-backend.html**: 고급 UI 처리 및 통합 테스트
3. **test-ai-root.html**: 정적 파일 버전 테스트

이 문서를 참고하여 SSE 기능을 구현하고 테스트할 수 있습니다.
