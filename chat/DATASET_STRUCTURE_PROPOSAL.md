# 대화 데이터셋 구축을 위한 구조 제안

## 📋 목적

모든 챗봇의 대화 데이터를 체계적으로 수집하고, 나중에 파인튜닝이나 평가를 위한 데이터셋을 쉽게 추출할 수 있도록 구조를 설계합니다.

---

## 🎯 현재 구조 분석

### 저장되는 데이터
1. **메시지**: `messages` 테이블에 사용자/봇 메시지 저장
2. **Agent 결과**: `messages.metadata` (JSONB)에 일부 Agent 결과 저장
3. **진척도**: `intimacy_progress` 테이블에 친밀도 진척도 및 이력 저장

### 누락되는 데이터
1. **Agent 실행 로그**: 각 Agent의 입력/출력이 별도로 저장되지 않음
2. **대화 컨텍스트**: 프롬프트에 사용된 전체 컨텍스트가 저장되지 않음
3. **에러 케이스**: 실패한 Agent 실행이 기록되지 않음
4. **토큰 사용량**: Agent별 토큰 사용량이 체계적으로 저장되지 않음

---

## 🏗️ 제안하는 구조

### 1. Agent 실행 로그 테이블

각 Agent의 실행을 독립적으로 기록하여 나중에 데이터셋 추출 시 유연하게 사용할 수 있도록 합니다.

```sql
CREATE TABLE chat_schema.agent_execution_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chatroom_id UUID NOT NULL,
    message_id UUID,  -- 사용자 메시지 ID (nullable)
    bot_message_id UUID,  -- 봇 메시지 ID (nullable)
    
    -- Agent 정보
    agent_type VARCHAR(50) NOT NULL,  -- intimacy, vocabulary, conversation, summarizer, translation
    agent_version VARCHAR(20),  -- 프롬프트 버전 등
    
    -- 입력 데이터
    input_data JSONB NOT NULL,  -- Agent에 전달된 입력
    input_prompt TEXT,  -- 실제 사용된 프롬프트 (선택적)
    
    -- 출력 데이터
    output_data JSONB,  -- Agent 응답 (성공 시)
    output_raw TEXT,  -- 원시 응답 (JSON 파싱 전)
    
    -- 실행 메타데이터
    execution_time_ms INTEGER,  -- 실행 시간
    token_usage JSONB,  -- {prompt_tokens, completion_tokens, total_tokens}
    model_name VARCHAR(100),  -- 사용된 모델 (gpt-4o-mini 등)
    temperature DOUBLE PRECISION,  -- 사용된 temperature
    
    -- 상태
    status VARCHAR(20) NOT NULL DEFAULT 'success',  -- success, failed, timeout
    error_message TEXT,  -- 실패 시 에러 메시지
    error_stack TEXT,  -- 실패 시 스택 트레이스
    
    -- 컨텍스트
    context_snapshot JSONB,  -- 실행 시점의 컨텍스트 스냅샷
    chatbot_id UUID,  -- 챗봇 ID
    user_id UUID,  -- 사용자 ID
    
    created_at TIMESTAMP DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_agent_logs_chatroom ON chat_schema.agent_execution_logs(chatroom_id);
CREATE INDEX idx_agent_logs_message ON chat_schema.agent_execution_logs(message_id);
CREATE INDEX idx_agent_logs_agent_type ON chat_schema.agent_execution_logs(agent_type);
CREATE INDEX idx_agent_logs_created ON chat_schema.agent_execution_logs(created_at);
CREATE INDEX idx_agent_logs_status ON chat_schema.agent_execution_logs(status);
CREATE INDEX idx_agent_logs_chatbot ON chat_schema.agent_execution_logs(chatbot_id);
```

**input_data 예시:**
```json
{
  "chatroomId": "uuid",
  "userMessage": "오늘 밥 먹었어?",
  "concept": "FRIEND",
  "intimacyLevel": 1,
  "previousContext": "..."
}
```

**output_data 예시 (IntimacyAgent):**
```json
{
  "detectedLevel": 1,
  "correctedSentence": "오늘 밥 드셨어요?",
  "feedback": {
    "ko": "친밀도 레벨 1에 맞게 격식체로 교정했습니다.",
    "en": "Corrected to formal speech for intimacy level 1."
  },
  "corrections": "밥 먹었어 → 밥 드셨어요"
}
```

**context_snapshot 예시:**
```json
{
  "chatroomSettings": {
    "concept": "FRIEND",
    "intimacyLevel": 1
  },
  "recentMessages": [
    {"role": "user", "content": "안녕"},
    {"role": "bot", "content": "안녕하세요!"}
  ],
  "summaryHistory": [...],
  "keywordIndex": [...]
}
```

---

### 2. 대화 세션 테이블

대화의 전체 흐름을 세션 단위로 관리하여 데이터셋 추출 시 연속된 대화를 쉽게 가져올 수 있도록 합니다.

```sql
CREATE TABLE chat_schema.conversation_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chatroom_id UUID NOT NULL,
    chatbot_id UUID NOT NULL,
    user_id UUID NOT NULL,
    
    -- 세션 정보
    session_start_at TIMESTAMP NOT NULL,
    session_end_at TIMESTAMP,
    message_count INTEGER DEFAULT 0,
    
    -- 세션 메타데이터
    session_metadata JSONB,  -- 세션 시작 시점의 설정 등
    final_summary TEXT,  -- 세션 종료 시 요약 (선택적)
    
    -- 데이터셋 관련
    is_exported BOOLEAN DEFAULT false,  -- 데이터셋으로 추출되었는지
    exported_at TIMESTAMP,
    export_version VARCHAR(50),  -- 추출 버전/태그
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_sessions_chatroom ON chat_schema.conversation_sessions(chatroom_id);
CREATE INDEX idx_sessions_chatbot ON chat_schema.conversation_sessions(chatbot_id);
CREATE INDEX idx_sessions_user ON chat_schema.conversation_sessions(user_id);
CREATE INDEX idx_sessions_exported ON chat_schema.conversation_sessions(is_exported);
```

---

### 3. 데이터셋 추출 테이블

추출된 데이터셋의 메타데이터를 관리합니다.

```sql
CREATE TABLE chat_schema.dataset_exports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset_name VARCHAR(200) NOT NULL,
    dataset_version VARCHAR(50) NOT NULL,
    dataset_type VARCHAR(50) NOT NULL,  -- finetuning, evaluation, analysis
    
    -- 필터 조건 (JSONB)
    filter_criteria JSONB,  -- {chatbotIds: [...], dateRange: {...}, agentTypes: [...]}
    
    -- 통계
    total_sessions INTEGER,
    total_messages INTEGER,
    total_agent_executions INTEGER,
    chatbot_distribution JSONB,  -- 챗봇별 분포
    
    -- 파일 정보
    file_path TEXT,  -- 저장된 파일 경로
    file_format VARCHAR(20),  -- jsonl, json, csv
    file_size_bytes BIGINT,
    
    -- 메타데이터
    description TEXT,
    created_by UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_exports_name_version ON chat_schema.dataset_exports(dataset_name, dataset_version);
CREATE INDEX idx_exports_type ON chat_schema.dataset_exports(dataset_type);
CREATE INDEX idx_exports_created ON chat_schema.dataset_exports(created_at);
```

---

## 🔧 구현 방안

### 1. Agent 실행 로깅 서비스

모든 Agent 실행을 자동으로 로깅하는 서비스를 만듭니다.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentExecutionLogger {
    private final AgentExecutionLogRepository logRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Agent 실행 로그 저장
     */
    public <T> Mono<T> logExecution(
        String agentType,
        UUID chatroomId,
        UUID messageId,
        Object inputData,
        Mono<T> agentExecution,
        String modelName,
        Double temperature
    ) {
        long startTime = System.currentTimeMillis();
        UUID logId = UUID.randomUUID();
        
        // 컨텍스트 스냅샷 생성
        JsonNode contextSnapshot = buildContextSnapshot(chatroomId);
        
        // 입력 데이터 직렬화
        String inputJson = serializeInput(inputData);
        
        return agentExecution
            .doOnSubscribe(s -> {
                // 로그 시작 기록 (비동기)
                saveLogStart(logId, agentType, chatroomId, messageId, inputJson, contextSnapshot, modelName, temperature);
            })
            .doOnSuccess(result -> {
                long duration = System.currentTimeMillis() - startTime;
                // 성공 로그 저장
                saveLogSuccess(logId, result, duration, extractTokenUsage(result));
            })
            .doOnError(error -> {
                long duration = System.currentTimeMillis() - startTime;
                // 실패 로그 저장
                saveLogFailure(logId, error, duration);
            });
    }
    
    private JsonNode buildContextSnapshot(UUID chatroomId) {
        // ChatRoom, 최근 메시지, IntimacyProgress 등 조회하여 스냅샷 생성
        // ...
    }
}
```

### 2. MultiAgentOrchestrator 수정

기존 Orchestrator에 로깅을 추가합니다.

```java
// MultiAgentOrchestrator.java 수정 예시
Mono<IntimacyAgentResponse> intimacyMono = agentExecutionLogger.logExecution(
    "intimacy",
    chatroomId,
    userMessage.getId(),
    Map.of("userMessage", content, "chatroomId", chatroomId),
    intimacyAgent.analyze(chatroomId, content),
    aiConfig.getModelName(),
    aiConfig.getTemperature()
);
```

### 3. 데이터셋 추출 서비스

나중에 데이터셋을 추출하는 서비스를 만듭니다.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DatasetExportService {
    private final AgentExecutionLogRepository logRepository;
    private final ConversationSessionRepository sessionRepository;
    private final DatasetExportRepository exportRepository;
    
    /**
     * 파인튜닝용 데이터셋 추출
     */
    public DatasetExport exportForFinetuning(
        String datasetName,
        String version,
        DatasetFilter filter
    ) {
        // 1. 필터 조건에 맞는 Agent 실행 로그 조회
        List<AgentExecutionLog> logs = logRepository.findByFilter(filter);
        
        // 2. 데이터셋 형식으로 변환
        List<FinetuningExample> examples = logs.stream()
            .map(this::convertToFinetuningExample)
            .collect(Collectors.toList());
        
        // 3. JSONL 파일로 저장
        String filePath = saveAsJsonl(datasetName, version, examples);
        
        // 4. 메타데이터 저장
        DatasetExport export = DatasetExport.builder()
            .datasetName(datasetName)
            .datasetVersion(version)
            .datasetType("finetuning")
            .filterCriteria(objectMapper.valueToTree(filter))
            .totalAgentExecutions(examples.size())
            .filePath(filePath)
            .fileFormat("jsonl")
            .build();
        
        return exportRepository.save(export);
    }
    
    /**
     * 평가용 데이터셋 추출
     */
    public DatasetExport exportForEvaluation(
        String datasetName,
        String version,
        DatasetFilter filter
    ) {
        // 평가용 형식으로 변환 (입력-기대출력 쌍)
        // ...
    }
    
    private FinetuningExample convertToFinetuningExample(AgentExecutionLog log) {
        // Agent 타입별로 다른 형식으로 변환
        return switch (log.getAgentType()) {
            case "intimacy" -> convertIntimacyExample(log);
            case "vocabulary" -> convertVocabularyExample(log);
            case "conversation" -> convertConversationExample(log);
            default -> throw new IllegalArgumentException("Unknown agent type: " + log.getAgentType());
        };
    }
}
```

---

## 📊 데이터셋 형식 예시

### 파인튜닝용 (JSONL)

**IntimacyAgent 예시:**
```jsonl
{"messages": [{"role": "system", "content": "..."}, {"role": "user", "content": "오늘 밥 먹었어?"}], "expected": {"detectedLevel": 1, "correctedSentence": "오늘 밥 드셨어요?"}}
{"messages": [{"role": "system", "content": "..."}, {"role": "user", "content": "안녕"}], "expected": {"detectedLevel": 1, "correctedSentence": "안녕하세요"}}
```

**ConversationAgent 예시:**
```jsonl
{"messages": [{"role": "system", "content": "..."}, {"role": "user", "content": "오늘 날씨 어때?"}], "expected": "오늘 날씨가 정말 좋네요!"}
```

### 평가용 (JSON)

```json
{
  "dataset_name": "intimacy_evaluation_v1",
  "version": "1.0.0",
  "test_cases": [
    {
      "id": "intimacy_001",
      "agent_type": "intimacy",
      "input": {
        "userMessage": "오늘 밥 먹었어?",
        "concept": "FRIEND",
        "intimacyLevel": 1
      },
      "expected": {
        "detectedLevel": 1,
        "correctedSentence": "오늘 밥 드셨어요?"
      },
      "actual": {
        "detectedLevel": 1,
        "correctedSentence": "오늘 밥 드셨어요?",
        "execution_time_ms": 1200,
        "token_usage": {"total": 350}
      }
    }
  ]
}
```

---

## 🎯 구현 우선순위

### Phase 1: 기본 로깅 (즉시)
1. ✅ `agent_execution_logs` 테이블 생성
2. ✅ `AgentExecutionLogger` 서비스 구현
3. ✅ `MultiAgentOrchestrator`에 로깅 추가
4. ✅ 각 Agent 호출 시 자동 로깅

### Phase 2: 세션 관리 (1-2주)
1. ✅ `conversation_sessions` 테이블 생성
2. ✅ 세션 시작/종료 로직 추가
3. ✅ 세션별 통계 수집

### Phase 3: 데이터셋 추출 (2-3주)
1. ✅ `dataset_exports` 테이블 생성
2. ✅ `DatasetExportService` 구현
3. ✅ 파인튜닝용 데이터셋 추출 API
4. ✅ 평가용 데이터셋 추출 API

### Phase 4: 고급 기능 (선택적)
1. ⚠️ 데이터셋 품질 검증
2. ⚠️ 자동 데이터셋 생성 스케줄링
3. ⚠️ 데이터셋 버전 관리

---

## 💡 추가 고려사항

### 1. 개인정보 보호
- 데이터셋 추출 시 개인정보 마스킹 옵션
- 사용자 동의 필수

### 2. 성능
- 로깅은 비동기로 처리하여 대화 응답 지연 최소화
- 대량 데이터 조회 시 인덱스 활용

### 3. 저장 공간
- 오래된 로그는 별도 아카이브 테이블로 이동
- 필요 시 파티셔닝 고려

### 4. 확장성
- 새로운 Agent 추가 시에도 동일한 로깅 구조 사용
- Agent별 커스텀 필드 지원 (JSONB 활용)

---

## 📝 결론

이 구조를 통해:
1. ✅ 모든 챗봇의 대화 데이터를 체계적으로 수집
2. ✅ Agent별 실행 로그를 독립적으로 관리
3. ✅ 나중에 다양한 목적의 데이터셋을 쉽게 추출
4. ✅ 파인튜닝, 평가, 분석 등 다양한 용도로 활용 가능

현재 구조에 최소한의 변경으로 추가할 수 있으며, 기존 기능에 영향을 주지 않습니다.



