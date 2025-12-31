# 챗봇 구조 분석 및 개선 가능성 리포트

## 📋 현재 구조 분석

### 1. 아키텍처 개요

현재 챗봇은 **멀티 에이전트 오케스트레이션 패턴**을 사용하고 있으며, 이는 제공하신 가이드의 "레벨 1: 시스템/오케스트레이션 레벨"에 해당합니다.

#### 핵심 구성 요소

1. **MultiAgentOrchestrator** (`MultiAgentOrchestrator.java`)
   - 여러 Agent를 병렬/순차로 조율
   - Reactor 기반 비동기 처리
   - SSE를 통한 실시간 스트리밍

2. **Agent 구성**
   - `ConversationAgent`: 자연스러운 대화 생성
   - `IntimacyAgent`: 친밀도 분석 및 문장 교정
   - `VocabularyAgent`: 어려운 어휘 추출 및 설명
   - `SummarizerAgent`: 대화 요약 및 키워드 추출

3. **프롬프트 관리**
   - 현재 상태: 각 Agent 클래스 내부에 하드코딩
   - 예시:
     - `IntimacyAgent.buildIntimacyPrompt()`: 900+ 줄의 프롬프트 문자열
     - `VocabularyAgent.buildVocabularyPrompt()`: 240+ 줄의 프롬프트 문자열
     - `PromptService.buildSystemPrompt()`: 동적 프롬프트 생성 (2000+ 줄)

4. **모델 설정**
   - 모델: ChatGPT 4.0 mini (OpenAI API)
   - 설정: `AIConfig`를 통한 중앙 관리
   - Temperature: 0.85 (고정)

5. **응답 형식**
   - JSON 구조화된 응답
   - 각 Agent별 명확한 Response DTO 정의

### 2. 현재 구조의 강점

✅ **잘 구현된 부분**
- 멀티 에이전트 패턴이 명확하게 분리되어 있음
- Reactor 기반 비동기 처리로 성능 최적화
- JSON 구조화된 응답으로 프론트엔드 통신 용이
- 각 Agent의 역할이 명확히 분리됨
- SSE를 통한 실시간 피드백 제공

---

## 🔍 개선 가능성 분석

### 레벨 1: 구조 정리 및 문서화 (즉시 가능)

#### 1.1 프롬프트 템플릿 분리

**현재 문제점:**
- 프롬프트가 Java 코드에 하드코딩되어 있음
- 수정 시 재컴파일 필요
- 버전 관리 어려움
- 프롬프트 A/B 테스트 불가

**개선 방안:**
```
chat/src/main/resources/
  └── prompts/
      ├── conversation/
      │   ├── base_system_prompt.txt
      │   ├── concept_friend_level1.txt
      │   ├── concept_friend_level3.txt
      │   └── concept_coworker_level1.txt
      ├── intimacy/
      │   ├── base_prompt.txt
      │   ├── guidelines_friend.txt
      │   └── guidelines_coworker.txt
      └── vocabulary/
          └── base_prompt.txt
```

**구현 예시:**
```java
@Service
public class PromptTemplateService {
    private final ResourceLoader resourceLoader;
    
    public String loadTemplate(String agentType, String templateName) {
        String path = String.format("prompts/%s/%s", agentType, templateName);
        return resourceLoader.getResource("classpath:" + path)
            .getContentAsString(StandardCharsets.UTF_8);
    }
    
    public String renderTemplate(String template, Map<String, Object> variables) {
        // Mustache 또는 StringTemplate 사용
        return template.replace("{{concept}}", variables.get("concept").toString());
    }
}
```

**예상 효과:**
- 프롬프트 수정 시 재배포 불필요 (파일만 업데이트)
- 프롬프트 버전 관리 용이
- A/B 테스트 가능

#### 1.2 Agent 입력/출력 스키마 문서화

**현재 상태:**
- Response DTO는 정의되어 있으나 API 스펙 문서 없음

**개선 방안:**
- OpenAPI/Swagger 스펙 추가
- 각 Agent별 Request/Response 스키마 문서화
- 예시 요청/응답 포함

**구현 예시:**
```java
@Schema(description = "IntimacyAgent 응답")
public record IntimacyAgentResponse(
    @Schema(description = "에이전트 타입", example = "intimacy")
    String agentType,
    
    @Schema(description = "감지된 친밀도 레벨", example = "1", allowableValues = {"1", "3"})
    int detectedLevel,
    
    @Schema(description = "교정된 문장", example = "오늘 밥 드셨어요?")
    String correctedSentence,
    
    // ...
) {}
```

#### 1.3 설정 외부화

**현재 문제점:**
- Temperature 등 하이퍼파라미터가 코드에 하드코딩
- Agent별 설정 분리 불가

**개선 방안:**
```yaml
# application.yml
ai:
  agents:
    conversation:
      temperature: 0.85
      max_tokens: 800
    intimacy:
      temperature: 0.3  # 더 일관된 응답을 위해 낮춤
      max_tokens: 400
    vocabulary:
      temperature: 0.2  # 정확한 추출을 위해 낮춤
      max_tokens: 300
```

---

### 레벨 2: 평가 파이프라인 구축 (중요도: 높음)

#### 2.1 테스트 셋 구성

**필요한 데이터:**
- 입력 → 기대 응답 형태의 데이터셋
- 각 Agent별 30~50개 테스트 케이스

**데이터 구조 예시:**
```json
{
  "testCases": [
    {
      "id": "intimacy_001",
      "agentType": "intimacy",
      "input": {
        "chatroomId": "test-room-1",
        "userMessage": "오늘 밥 먹었어?",
        "concept": "FRIEND",
        "currentLevel": 1
      },
      "expected": {
        "detectedLevel": 1,
        "correctedSentence": "오늘 밥 먹었어?",
        "corrections": "",
        "feedback": {
          "ko": "",
          "en": ""
        }
      }
    }
  ]
}
```

**구현 예시:**
```java
@Service
public class AgentEvaluationService {
    
    public EvaluationResult evaluateAgent(String agentType, List<TestCase> testCases) {
        List<TestResult> results = testCases.stream()
            .map(this::runTestCase)
            .collect(Collectors.toList());
        
        return EvaluationResult.builder()
            .totalCases(testCases.size())
            .passedCases(countPassed(results))
            .failedCases(countFailed(results))
            .accuracy(calculateAccuracy(results))
            .details(results)
            .build();
    }
    
    private TestResult runTestCase(TestCase testCase) {
        // Agent 실행 및 결과 비교
        AgentResponse actual = executeAgent(testCase);
        boolean passed = compareResponse(actual, testCase.getExpected());
        
        return TestResult.builder()
            .testCaseId(testCase.getId())
            .passed(passed)
            .actual(actual)
            .expected(testCase.getExpected())
            .build();
    }
}
```

#### 2.2 평가 지표 정의

**IntimacyAgent:**
- 정확도: detectedLevel 일치율
- 교정 정확도: correctedSentence가 기대값과 일치하는 비율
- 컨셉 준수율: 컨셉 제약 위반하지 않은 비율

**VocabularyAgent:**
- 추출 정확도: 올바른 단어를 추출한 비율
- 난이도 분류 정확도: difficulty 레벨이 올바른 비율
- 설명 품질: context.ko/en의 적절성 (수동 평가)

**ConversationAgent:**
- 응답 적절성: 문맥 유지, 말투 일관성 (수동 평가)
- 길이 적절성: 응답 길이가 적절한 비율

#### 2.3 평가 리포트 생성

**구현 예시:**
```java
@Service
public class EvaluationReportService {
    
    public void generateReport(EvaluationResult result, String outputPath) {
        Map<String, Object> data = Map.of(
            "timestamp", LocalDateTime.now(),
            "agentType", result.getAgentType(),
            "totalCases", result.getTotalCases(),
            "passedCases", result.getPassedCases(),
            "accuracy", result.getAccuracy(),
            "failedCases", result.getFailedCases().stream()
                .map(this::formatFailure)
                .collect(Collectors.toList())
        );
        
        // HTML 또는 Markdown 리포트 생성
        String report = renderTemplate("evaluation_report.html", data);
        Files.write(Paths.get(outputPath), report.getBytes());
    }
}
```

---

### 레벨 3: 로그 수집 및 분석 (중요도: 중간)

#### 3.1 대화 로그 저장

**현재 상태:**
- 메시지는 DB에 저장되지만 Agent 실행 로그는 별도 저장 안 됨

**개선 방안:**
```sql
CREATE TABLE chat_schema.agent_execution_logs (
    id UUID PRIMARY KEY,
    chatroom_id UUID REFERENCES chat_schema.chatrooms(id),
    agent_type VARCHAR(50),
    input_data JSONB,
    output_data JSONB,
    execution_time_ms INT,
    token_usage JSONB,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_agent_logs_chatroom ON chat_schema.agent_execution_logs(chatroom_id);
CREATE INDEX idx_agent_logs_type ON chat_schema.agent_execution_logs(agent_type);
CREATE INDEX idx_agent_logs_created ON chat_schema.agent_execution_logs(created_at);
```

**구현 예시:**
```java
@Aspect
@Component
public class AgentExecutionLogger {
    
    @Around("execution(* com.dorandoran.chat.service.agent.*.*(..))")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String agentType = extractAgentType(joinPoint);
        Object input = joinPoint.getArgs();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            saveLog(agentType, input, result, duration, null);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            saveLog(agentType, input, null, duration, e.getMessage());
            throw e;
        }
    }
}
```

#### 3.2 실패 케이스 분석

**구현 예시:**
```java
@Service
public class FailureAnalysisService {
    
    public FailureAnalysisResult analyzeFailures(
        String agentType, 
        LocalDateTime from, 
        LocalDateTime to
    ) {
        List<AgentExecutionLog> failures = logRepository
            .findFailures(agentType, from, to);
        
        Map<String, Long> errorPatterns = failures.stream()
            .collect(Collectors.groupingBy(
                log -> extractErrorPattern(log.getErrorMessage()),
                Collectors.counting()
            ));
        
        return FailureAnalysisResult.builder()
            .totalFailures(failures.size())
            .errorPatterns(errorPatterns)
            .commonFailures(extractCommonFailures(failures))
            .recommendations(generateRecommendations(errorPatterns))
            .build();
    }
}
```

---

### 레벨 4: 파인튜닝 가능성 (선택적)

#### 4.1 현재 모델 분석

**사용 중인 모델:**
- ChatGPT 4.0 mini (OpenAI API)

**파인튜닝 가능 여부:**
- ❌ **OpenAI API 모델은 파인튜닝 불가**
- OpenAI는 자체 모델의 파인튜닝을 제공하지 않음
- 대신 프롬프트 엔지니어링과 RAG를 통한 개선 권장

#### 4.2 파인튜닝이 필요한 경우

**파인튜닝이 의미 있는 경우:**
1. 자체 모델을 사용하는 경우 (예: Llama, Mistral)
2. 특정 도메인에 특화된 응답이 필요한 경우
3. 비용/지연시간 최적화가 필요한 경우
4. 프롬프트만으로는 품질/일관성이 안 나오는 경우

**현재 상황 평가:**
- ✅ 프롬프트 엔지니어링으로 충분히 개선 가능
- ✅ 프롬프트 템플릿 분리로 A/B 테스트 가능
- ❌ 파인튜닝은 현재 구조에서 불필요

#### 4.3 파인튜닝을 원한다면

**대안 1: 오픈소스 모델 사용**
```yaml
# 예시: Llama 3.1 8B 사용
ai:
  model:
    provider: "ollama"  # 또는 vLLM, Together AI 등
    model_name: "llama3.1:8b"
    base_url: "http://localhost:11434"
```

**대안 2: LoRA 파인튜닝**
- 작은 모델(8B 이하)에 LoRA 적용
- 특정 Agent만 파인튜닝 (예: VocabularyAgent만)
- 학습 데이터: 대화 로그에서 추출한 QA 페어

**구현 예시 (참고용):**
```python
# Python 스크립트 (별도 프로젝트)
from peft import LoraConfig, get_peft_model
from transformers import AutoModelForCausalLM

# LoRA 설정
lora_config = LoraConfig(
    r=16,
    lora_alpha=32,
    target_modules=["q_proj", "v_proj"],
    lora_dropout=0.1,
)

# 모델 로드 및 LoRA 적용
model = AutoModelForCausalLM.from_pretrained("meta-llama/Llama-3.1-8B")
model = get_peft_model(model, lora_config)

# 학습 데이터 준비
train_dataset = prepare_dataset_from_logs("vocabulary_agent_logs.jsonl")

# 학습 실행
trainer = Trainer(
    model=model,
    train_dataset=train_dataset,
    # ...
)
trainer.train()
```

**주의사항:**
- 파인튜닝은 별도의 ML 인프라 필요
- 학습 데이터 준비 및 라벨링 작업 필요
- 모델 서빙 인프라 구축 필요
- 현재 구조에서는 **프롬프트 개선이 더 효율적**

---

## 📊 우선순위별 개선 로드맵

### Phase 1: 즉시 개선 (1-2주)

1. ✅ **프롬프트 템플릿 분리**
   - `prompts/` 디렉토리 생성
   - 각 Agent별 템플릿 파일 분리
   - `PromptTemplateService` 구현

2. ✅ **설정 외부화**
   - `application.yml`에 Agent별 설정 추가
   - Temperature, max_tokens 등 하이퍼파라미터 외부화

3. ✅ **API 스펙 문서화**
   - OpenAPI/Swagger 추가
   - 각 Agent의 Request/Response 스키마 문서화

### Phase 2: 평가 파이프라인 (2-4주)

1. ✅ **테스트 셋 구성**
   - 각 Agent별 30-50개 테스트 케이스 작성
   - JSON 형식으로 저장

2. ✅ **평가 서비스 구현**
   - `AgentEvaluationService` 구현
   - 자동 평가 스크립트 작성

3. ✅ **평가 리포트 생성**
   - HTML/Markdown 리포트 템플릿
   - 통과/실패 케이스 분석

### Phase 3: 로그 및 분석 (2-3주)

1. ✅ **로그 저장**
   - `agent_execution_logs` 테이블 생성
   - AOP를 통한 자동 로깅

2. ✅ **실패 분석**
   - 실패 패턴 분석 서비스
   - 자주 실패하는 케이스 추출

3. ✅ **프롬프트 개선 리포트**
   - 프롬프트 변경 전후 비교
   - 성능 지표 변화 추적

### Phase 4: 선택적 - 파인튜닝 (4-8주)

1. ⚠️ **오픈소스 모델 도입 검토**
   - Llama/Mistral 등 모델 평가
   - 인프라 요구사항 확인

2. ⚠️ **LoRA 파인튜닝 실험**
   - VocabularyAgent만 선별 실험
   - 학습 데이터 준비
   - 성능 비교 리포트

---

## 🎯 결론 및 권장사항

### 현재 구조 평가

✅ **잘 구현된 부분:**
- 멀티 에이전트 오케스트레이션 패턴이 명확함
- Reactor 기반 비동기 처리로 성능 최적화
- JSON 구조화된 응답으로 프론트엔드 통신 용이

⚠️ **개선이 필요한 부분:**
- 프롬프트 하드코딩 → 템플릿 분리 필요
- 평가 파이프라인 부재 → 즉시 구축 권장
- 로그 수집 부재 → 분석을 위한 로깅 필요

### 권장 우선순위

1. **프롬프트 템플릿 분리** (가장 시급)
   - 프롬프트 수정 시 재배포 불필요
   - A/B 테스트 가능
   - 버전 관리 용이

2. **평가 파이프라인 구축** (중요)
   - 프롬프트 개선 효과 측정
   - 회귀 테스트 자동화
   - 품질 보장

3. **로그 수집 및 분석** (중간)
   - 실패 케이스 분석
   - 사용 패턴 파악
   - 프롬프트 개선 근거 확보

4. **파인튜닝** (선택적)
   - 현재는 불필요
   - 프롬프트 개선으로 충분
   - 필요 시 오픈소스 모델 도입 검토

### 최종 평가

현재 구조는 **"레벨 1: 시스템/오케스트레이션 레벨"**에 해당하며, 이는 상용 서비스에서도 충분히 사용되는 수준입니다. 

**제안하신 가이드대로:**
1. 프롬프트 템플릿 분리
2. 평가 파이프라인 구축
3. 로그 수집 및 분석

이 3가지만 구현해도 **"제대로 했다"**고 자신 있게 말할 수 있는 수준이 됩니다.

파인튜닝은 현재 구조에서는 불필요하며, 프롬프트 개선으로 충분히 품질 향상이 가능합니다.

