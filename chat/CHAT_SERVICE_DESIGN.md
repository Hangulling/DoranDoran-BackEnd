## Chat Service 설계 문서 (Multi-Agent AI, WebSocket + SSE) - 단순화 버전

### 목적
외국인 사용자의 한국어 학습을 지원하는 Multi-Agent AI 시스템을 구축한다. Parallel + Aggregator 패턴으로 친밀도 분석, 어휘 추출, 번역, 대화 Agent를 조율하여 실시간으로 학습 지원을 제공한다.

**Multi-Agent 원칙**: 병렬 처리로 성능 최적화, SSE 스트리밍으로 실시간 피드백, 친밀도 레벨 1-3 단계로 단순화

---

## 1. Multi-Agent AI 아키텍처

### 1.1 Agent 구성
- **IntimacyAgent**: 한국어 친밀도 분석 및 교정 (1=격식체, 2=부드러운 존댓말, 3=반말)
- **VocabularyAgent**: 어휘 난이도 분석 및 어려운 단어 추출 (최대 1개)
- **TranslationAgent**: 추출된 단어의 영어 번역 및 발음기호 제공
- **ConversationAgent**: 자연스러운 대화 응답 생성

### 1.2 처리 패턴
- **Phase 1 (병렬)**: IntimacyAgent, VocabularyAgent, ConversationAgent 동시 실행
- **Phase 2 (순차)**: VocabularyAgent 결과를 TranslationAgent에 전달
- **Phase 3 (스트림)**: ConversationAgent는 독립적으로 SSE 스트리밍
- **Phase 4 (집계)**: 모든 Agent 결과를 통합하여 최종 피드백 제공

### 1.3 SSE 이벤트 타입
- `intimacy_analysis`: 친밀도 분석 결과
- `vocabulary_extracted`: 어휘 추출 결과
- `vocabulary_translated`: 번역 결과
- `conversation_chunk`: 대화 응답 스트림
- `conversation_complete`: 대화 완료
- `aggregated_complete`: 전체 결과 집계

---

## 2. 데이터베이스 스키마 (chat_schema) - 단순화

### 1.1 chatbots
설명: AI 챗봇 메타 정보를 관리한다. 다양한 모델/인격/기능을 가진 챗봇을 유연하게 확장하기 위함.

스키마(의도 기반 요약):
- id UUID PK, name, display_name, description
- bot_type (예: gpt, claude, custom), model_name
- personality JSONB, system_prompt TEXT, capabilities JSONB, settings JSONB
- intimacy_level INT(1~3), avatar_url, is_active
- created_at, updated_at, created_by (user_schema.app_user 참조)

WHY:
- bot_type/model_name: 외부 AI 제공자/모델 변경에 따른 라우팅/옵션 분기를 단순화.
- personality/system_prompt: 동일 모델이라도 다른 페르소나를 빠르게 구성하기 위함.
- capabilities/settings(JSONB): 모델별 세부 옵션과 실행 제어를 스키마 변경 없이 확장.
- created_by: 운영 도구에서 누가 챗봇을 생성/관리했는지 감사 추적.

**capabilities JSONB 상세 가이드**:
```json
{
  "model": "gpt-4o-mini",                    // AI 모델명
  "modalities": ["text"],                    // 지원 모달리티 (text만)
  "tools": [],                               // 함수 호출 도구 (추후 확장)
  "temperature": 0.7,                        // 창의성 (0.0-2.0)
  "topP": 1.0,                              // 토큰 선택 다양성 (0.0-1.0)
  "maxTokens": 800,                          // 최대 응답 토큰 수
  "safety": {                                // 안전 필터 설정
    "profanityFilter": true,                 // 욕설 필터
    "piiRedaction": true,                    // 개인정보 마스킹
    "harmfulContent": "block"                // 유해 콘텐츠 차단
  },
  "responseStyle": {                         // 응답 스타일 제어
    "format": "markdown",                    // 마크다운 포맷
    "bulletPreference": "concise",           // 불릿 포인트 선호도
    "maxLength": 500,                        // 최대 응답 길이
    "includeExamples": true                  // 예시 포함 여부
  },
  "rateLimits": {                            // 속도 제한
    "rpm": 60,                               // 분당 요청 수
    "tpm": 40000,                            // 분당 토큰 수
    "dailyLimit": 1000                       // 일일 요청 제한
  }
}
```

**personality JSONB 상세 가이드**:
```json
{
  "traits": ["친절함", "간결함", "문맥충실"],     // 챗봇 성격 특성
  "speakingStyle": {                         // 말투 스타일
    "honorific": true,                       // 존댓말 사용
    "emoji": "minimal",                      // 이모지 사용도 (none/minimal/moderate/heavy)
    "formality": "polite",                   // 격식도 (casual/polite/formal)
    "length": "concise"                      // 답변 길이 (brief/concise/detailed)
  },
  "domainKnowledge": ["헬스케어", "일정관리"],    // 전문 도메인
  "guardrails": {                            // 안전 가드레일
    "refuseTopics": ["정치적 편향", "의료진단 단정"], // 거부 주제
    "escalationHint": "전문가 상담 권유",        // 에스컬레이션 안내
    "uncertaintyHandling": "admit"           // 불확실성 처리 (admit/avoid/redirect)
  },
  "systemPrompt": "당신은 도란도란의 AI 어시스턴트입니다...", // 시스템 프롬프트
  "fewShot": [                               // Few-shot 예시
    {
      "user": "오늘 기분이 안 좋아요",
      "assistant": "기분이 안 좋으시군요. 어떤 일이 있었는지 말씀해 주시면 도움을 드릴게요."
    }
  ],
  "customInstructions": {                    // 커스텀 지시사항
    "alwaysAskClarification": true,          // 항상 명확화 요청
    "provideSources": false,                 // 출처 제공 여부
    "suggestNextSteps": true                 // 다음 단계 제안
  }
}
```

---

### 1.2 chatrooms
설명: 사용자와 챗봇 간 1:1 대화 채널. 컨텍스트 정보를 통합하여 관리.

스키마(의도 기반 요약):
- id UUID PK, name, description
- chatbot_id(chatbots FK), user_id(user_schema.app_user FK)
- settings JSONB, context_data JSONB, last_message_at, last_message_id
- is_archived, is_deleted, created_at, updated_at

WHY:
- 1:1 대화에 집중하여 room_type 제거 (단순화).
- settings JSONB: 룸별 토큰 제한, 안전모드 등 런타임 정책을 유연히 적용.
- context_data JSONB: 대화 컨텍스트, 사용자 선호도, 세션 데이터를 통합 저장하여 조회 비용 최소화.
- last_message_*: 목록 화면 정렬/미리보기 성능 최적화(리스트 쿼리에서 조인 최소화).

**context_data JSONB 상세 가이드**:
```json
{
  "conversationSummary": "사용자가 건강 관리에 대해 문의...", // 대화 요약
  "userPreferences": {                       // 사용자 선호도
    "responseLength": "concise",             // 선호 응답 길이
    "language": "ko",                        // 선호 언어
    "topics": ["건강", "일정관리"]            // 관심 주제
  },
  "sessionData": {                           // 세션 데이터
    "currentTopic": "건강 관리",              // 현재 주제
    "conversationCount": 15,                 // 대화 횟수
    "lastActiveTime": "2024-01-15T10:30:00Z" // 마지막 활동 시간
  },
  "aiContext": {                             // AI 컨텍스트
    "memory": ["사용자는 매일 운동을 하고 싶어함"], // 기억할 정보
    "conversationHistory": [                 // 최근 대화 히스토리 (요약)
      {"role": "user", "content": "운동 계획을 세우고 싶어요"},
      {"role": "assistant", "content": "어떤 종류의 운동을 선호하시나요?"}
    ]
  }
}
```

인덱스 제안:
- UNIQUE(user_id, chatbot_id) WHERE NOT is_deleted: 1:1 룸 중복 방지.
- (user_id), (chatbot_id), (last_message_at DESC)

---

### 1.3 messages
설명: 대화의 단위 메시지. 텍스트 중심으로 단순화.

스키마(의도 기반 요약):
- id UUID PK, chatroom_id FK
- sender_type('user'|'bot'|'system'), sender_id UUID
- content TEXT, content_type('text'|'code'|'system'|'json')
- metadata JSONB, parent_message_id(self FK)
- sequence_number BIGINT NOT NULL
- token_count, processing_time_ms
- is_edited, edited_at, is_deleted, deleted_at
- created_at, updated_at

WHY:
- TEXT 사용: 100자 제약 제거, 긴 답변/코드/시스템 메세지 대응.
- content_type 확장: Multi-Agent 결과 저장을 위해 json 타입 추가.
- metadata: 코드 언어, 시스템 메시지 타입 등 가벼운 확장 정보만 저장.
- sequence_number: 룸 내 정렬 안정성 보장(타임스탬프 충돌/클럭 드리프트 방지).
- parent_message_id: 답글/스레드 기능을 위한 확장성 유지.
- token_count/processing_time_ms: 비용/성능 모니터링 및 레이트/쿼터 정책 근거 데이터.
- status 필드 제거: 1:1 대화에서 읽음 상태 추적 불필요.

**metadata JSONB 상세 가이드**:
```json
{
  "codeLanguage": "python",                  // 코드 언어 (content_type이 'code'일 때)
  "systemMessageType": "error",              // 시스템 메시지 타입 (error/info/warning)
  "aiModel": "gpt-4o-mini",                 // AI 모델명 (bot 메시지일 때)
  "confidence": 0.95,                        // AI 응답 신뢰도 (0.0-1.0)
  "processingTime": 1200,                    // 처리 시간 (밀리초)
  "tokenUsage": {                           // 토큰 사용량
    "prompt": 150,
    "completion": 200,
    "total": 350
  }
}
```

인덱스 제안:
- (chatroom_id, sequence_number), (sender_id), (created_at), (parent_message_id)
- 대용량 시 created_at 파티셔닝(월/주 단위) 고려.

---

## 제거된 테이블들

### ❌ message_status (제거)
**이유**: 1:1 대화에서 읽음/전달 상태 추적이 UX 핵심이 아니며, 서버·DB 부하 대비 이득이 작음.

### ❌ conversation_contexts (제거)
**이유**: 컨텍스트 정보를 `chatrooms.context_data`에 통합하여 조회·갱신 비용 최소화.

### ❌ file_attachments (제거)
**이유**: 파일 기능 제거로 메시지 모델이 단순해지고 저장/보안/스캔 파이프라인 복잡도 제거.

---

## 2. 런타임 아키텍처 (WebSocket + SSE)

### 구성요소
- WebSocket: 양방향 이벤트(사용자 입력, 타이핑 인디케이터, 상태 변경)에 최적.
- SSE: AI 토큰 스트리밍/시스템 알림을 서버→클라이언트 단방향으로 효율 전송.
- Redis Pub/Sub(선택): 멀티 인스턴스 간 세션/이벤트 브로드캐스트.
- AI Service: 외부 LLM 호출, 스트리밍 응답을 SSE로 전달.

WHY 선택 배경:
- WebSocket만으로도 가능하나, SSE는 브라우저 호환성과 서버 리소스 비용이 낮아 AI 스트리밍에 유리.
- 양방향(WS)과 단방향(SSE) 역할 분리로 복잡도 완화 및 장애 격리.

엔드포인트 가이드(예시):
- WebSocket: `/ws/chat/{chatroomId}`
- SSE: `GET /api/chat/stream/{chatroomId}` (text/event-stream)

메시지 포맷(예시):
- 클라이언트→서버(WS)
  - `{ "type": "message", "data": { "content": "안녕", "contentType": "text", "metadata": {} } }`
- 서버→클라이언트(SSE)
  - `event: ai_response`, `data: { "content": "...", "isComplete": false, "tokenCount": 12 }`

오케스트레이션:
1) 사용자 메시지 수신(WS) → `messages` 저장(sequence_number 채번) → 알림 브로드캐스트
2) AI 호출 비동기 수행 → 토큰 단위로 SSE 스트림 전송 → 최종 응답 저장/업데이트
3) 상태 변경(`message_status`) 이벤트를 WS로 전달(읽음/전달/실패 등)

---

## 3. API 가이드라인(요약)

- 채팅방 목록: `GET /api/chat/chatrooms?page=0&size=20`
- 채팅방 생성: `POST /api/chat/chatrooms`
- 메시지 조회: `GET /api/chat/chatrooms/{chatroomId}/messages?page=0&size=50`
- 메시지 전송: `POST /api/chat/chatrooms/{chatroomId}/messages`
- SSE 연결: `GET /api/chat/stream/{chatroomId}`

보안/권한:
- 게이트웨이 JWT 검증 → Chat 서비스에서 `user_id` 컨텍스트화.
- 룸 접근 검증: `existsByUserIdAndId(userId, chatroomId)`.
- 메시지 수정/삭제 권한: 작성자 기준.

---

## 4. 성능 및 운영

인덱스/파티셔닝:
- `messages(chatroom_id, sequence_number)` 필수.
- 대량 데이터 시 월 단위 파티셔닝 + 부분 인덱스(`WHERE NOT is_deleted`).

캐싱:
- 룸 메타/최근 메시지/컨텍스트는 Redis 캐시(만료 정책 기반)로 레이턴시 단축.

백프레셔/스루풋:
- AI 동시 처리 세마포어, 메시지 배치 저장(예: 100ms 간격 그룹 저장)으로 DB/IO 부하 완화.

모니터링:
- 메시지 처리 시간, AI 첫 토큰 시간, 활성 WS 연결 수, 에러율을 메트릭으로 수집.

---

## 5. 마이그레이션 가이드(요약)

1) 기존 `chat_schema.message` → `messages`로 마이그레이션 시 컬럼 맵핑 표 작성
2) `messege_id` 오타 컬럼 정리 및 `sequence_number` 채번 전략 추가
3) `JSON` 컬럼은 `JSONB`로 전환, 필요한 경우 GIN 인덱스 검토
4) 애플리케이션 레이어에서 엔티티/DTO 동기화 후 `ddl-auto=validate` 유지

---

## 6. 향후 옵션(제거/보류 가능 항목)

- `message_status`: 1:1에서 불필요하면 제거 가능. 향후 그룹/푸시에 필요.
- `conversation_contexts`: 초기에는 룸 `settings/context_data`에 통합 후 분리 가능.
- `file_attachments`: 파일 기능 미도입 시 테이블 생성 보류.
- `capabilities/personality`: MVP에서는 단순 `system_prompt`만 두고 축소 가능.

각 항목은 기능 복잡도/개발 속도/운영 비용을 고려해 단계적으로 도입한다.

---

## 3. Multi-Agent AI 아키텍처 상세

### 3.1 intimacy_progress (신규)
설명: 채팅방별 친밀도 진척을 추적한다. Multi-Agent AI의 IntimacyAgent 분석 결과를 저장하고 학습 진척도를 측정하기 위함.

스키마(의도 기반 요약):
- id UUID PK, chatroom_id FK, user_id FK, intimacy_level INT(1~3)
- total_corrections INT, last_feedback TEXT, last_updated TIMESTAMP
- progress_data JSONB

WHY:
- intimacy_level: 1-3 단계로 단순화 (1=격식체, 2=부드러운 존댓말, 3=반말)
- total_corrections: 교정 횟수로 학습 진척도 측정
- progress_data: 세부 학습 통계 (레벨별 교정 빈도, 자주 틀리는 패턴 등)
- 채팅방별 유니크 제약: 한 채팅방당 하나의 진척도만 추적

### 3.2 Agent 처리 흐름
1. **사용자 메시지 수신** → ChatController
2. **병렬 Agent 실행**:
   - IntimacyAgent: 친밀도 분석 및 교정
   - VocabularyAgent: 어려운 단어 추출 (최대 1개)
   - ConversationAgent: 자연스러운 대화 응답
3. **순차 처리**: VocabularyAgent → TranslationAgent
4. **SSE 스트리밍**: 각 Agent 완료 시 즉시 전송
5. **진척도 업데이트**: IntimacyAgent 결과로 학습 진척 추적

### 3.3 SSE 이벤트 타입 상세
- `intimacy_analysis`: 친밀도 분석 결과 (detectedLevel, correctedSentence, feedback)
- `vocabulary_extracted`: 어휘 추출 결과 (words 배열)
- `vocabulary_translated`: 번역 결과 (translations 배열)
- `conversation_chunk`: 대화 응답 스트림 (실시간 텍스트)
- `conversation_complete`: 대화 완료 (messageId, content)
- `aggregated_complete`: 전체 결과 집계 (intimacy, vocabulary 통합)


