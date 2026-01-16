# VocabularyAgent 고도화 계획 평가 및 개선 방안

## 📋 현재 VocabularyAgent 분석

### 현재 역할
- **입력**: 챗봇 응답 문장 (botResponse)
- **출력**: 어려운 단어/표현 1개 추출
- **기능**:
  - 단어/표현 추출
  - 동사원형 변환
  - 난이도 분류 (1-3)
  - 설명 생성 (ko, en)
  - 로마자 표기

### 현재 한계점
1. ❌ 컨셉(FRIEND, COWORKER 등) 고려 없음
2. ❌ 도메인별 어휘 분류 없음 (MZ세대, 비즈니스, 대학교 등)
3. ❌ 설명의 말투가 컨셉에 맞지 않음
4. ❌ Temperature가 고정 (0.85)

---

## ✅ 제안하신 계획 평가

### 1. ReasoningAgent와 StyleAgent 분리

**평가: ✅ 좋은 아이디어**

**장점:**
- 관심사 분리 (Separation of Concerns)
- 각 Agent의 역할이 명확해짐
- 독립적인 개선/테스트 가능

**개선 제안:**
- **명칭 변경 제안**: `VocabularyReasoningAgent` → `VocabularyExtractionAgent`
  - 이유: "Reasoning"은 추론을 의미하지만, 실제로는 "추출" 역할
- **StyleAgent 명칭**: `VocabularyStyleAgent` 또는 `VocabularyExplanationAgent`
  - 이유: "Style"은 말투를 의미하지만, 실제로는 "설명 생성" 역할

**권장 구조:**
```
VocabularyExtractionAgent (추출)
  ↓ JSON 전달
VocabularyExplanationAgent (설명 생성)
  ↓ 최종 응답
VocabularyAgentResponse
```

---

### 2. 컨셉별 추출 기준

**평가: ✅ 매우 좋은 아이디어**

**현재 문제:**
- 모든 컨셉에서 동일한 기준으로 추출
- 예: "국룰"은 FRIEND에서는 추출하지만 COWORKER에서는 추출하지 않아야 함

**개선 방안:**

#### 2.1 컨셉별 어휘 카테고리 정의

```java
public enum VocabularyCategory {
    // 공통
    BASIC_DAILY("일상 기본 어휘"),
    HANJA_BASED("한자어 기반 어휘"),
    
    // 컨셉별 특화
    MZ_GENERATION("MZ세대 유행어", Set.of("FRIEND", "HONEY")),
    BUSINESS_TERMS("비즈니스 용어", Set.of("COWORKER", "BOSS")),
    UNIVERSITY_TERMS("대학교 용어", Set.of("SENIOR")),
    SLANG("속어/신조어", Set.of("FRIEND", "HONEY")),
    FORMAL_EXPRESSIONS("격식 표현", Set.of("COWORKER", "BOSS", "SENIOR"));
    
    private final String description;
    private final Set<String> applicableConcepts;
}
```

#### 2.2 컨셉별 추출 우선순위

```java
// FRIEND 컨셉
- MZ세대 유행어 우선 추출 (예: "국룰", "레알", "개좋아")
- 속어/신조어 허용
- 비즈니스 용어는 낮은 우선순위

// COWORKER/BOSS 컨셉
- 비즈니스 용어 우선 추출 (예: "결재", "품의", "보고")
- 격식 표현 우선
- MZ세대 유행어는 제외

// SENIOR 컨셉
- 대학교 용어 우선 추출 (예: "족보", "과제", "출석")
- 격식 표현 허용
- 속어는 제외
```

---

### 3. StyleAgent 역할 명확화

**평가: ⚠️ 역할 명확화 필요**

**현재 계획의 모호한 점:**
- "말투만 담당" → 어떤 말투인가?
  - 설명(context.ko, context.en)의 말투?
  - 추출된 단어 자체의 말투?

**개선 제안:**

#### 3.1 역할 재정의

**VocabularyExplanationAgent (설명 생성 Agent)**
- **입력**: VocabularyExtractionAgent의 JSON 결과
- **출력**: 컨셉/레벨에 맞는 말투로 설명 생성
- **기능**:
  - context.ko: 컨셉/레벨에 맞는 말투로 설명
  - context.en: 일관된 톤으로 영어 설명
  - 예시 문장 생성

#### 3.2 말투 규칙 예시

```java
// FRIEND Level 3
context.ko: "'국룰'은 '국민 룰'의 줄임말이야. '당연한 것', '기본'이라는 뜻으로 쓰는 신조어야."
→ 반말, 친근한 톤

// COWORKER Level 1
context.ko: "'결재'는 직장 상사에게 서류나 계획을 보여드리고 승인받는 것을 말합니다."
→ 격식체, 정중한 톤

// SENIOR Level 3
context.ko: "'족보'는 선배들이 만든 과거 시험 문제나 답안을 말해요. 대학교에서 자주 쓰는 용어예요."
→ 부드러운 존댓말, 친근한 톤
```

---

### 4. 말투 평가 및 리라이트 (선택)

**평가: ✅ 유용하지만 복잡도 고려 필요**

**장점:**
- 설명 품질 향상
- 컨셉/레벨 일관성 보장

**단점:**
- 추가 API 호출 (비용 증가)
- 지연시간 증가
- 복잡도 증가

**개선 제안:**

#### 4.1 단계적 접근

**Phase 1: 간단한 검증 (권장)**
```java
// StyleAgent 내부에서 자체 검증
public VocabularyExplanationResult generateExplanation(
    VocabularyExtractionResult extraction,
    String concept,
    int intimacyLevel
) {
    String explanation = generateExplanationText(extraction, concept, intimacyLevel);
    
    // 간단한 검증 (규칙 기반)
    if (!isToneAppropriate(explanation, concept, intimacyLevel)) {
        explanation = rewriteExplanation(explanation, concept, intimacyLevel);
    }
    
    return new VocabularyExplanationResult(explanation, ...);
}
```

**Phase 2: LLM 기반 평가 (선택)**
```java
// 별도 평가 Agent (필요 시만)
public boolean evaluateTone(String explanation, String concept, int level) {
    // LLM으로 말투 평가
    // 비용/지연시간 고려하여 선택적 사용
}
```

#### 4.2 비용 최적화

- **캐싱**: 동일한 단어/컨셉/레벨 조합은 캐시
- **배치 처리**: 여러 단어를 한 번에 평가
- **임계값 설정**: 확신도가 낮을 때만 리라이트

---

### 5. 에이전트별 Temperature 구분

**평가: ✅ 매우 좋은 아이디어**

**현재 문제:**
- 모든 Agent가 temperature 0.85 사용
- 추출은 정확도가 중요 (낮은 temperature)
- 설명은 자연스러움이 중요 (적당한 temperature)

**개선 방안:**

```yaml
# application.yml
ai:
  agents:
    vocabulary-extraction:
      temperature: 0.2  # 정확한 추출을 위해 낮춤
      max_tokens: 300
    vocabulary-explanation:
      temperature: 0.5  # 자연스러운 설명을 위해 적당히
      max_tokens: 200
```

**구현 예시:**
```java
@Service
public class VocabularyExtractionAgent {
    private final AIConfig aiConfig;
    
    public Mono<VocabularyExtractionResult> extract(
        String botResponse,
        String concept,
        int intimacyLevel
    ) {
        // 컨셉별 추출 기준 주입
        String systemPrompt = buildExtractionPrompt(concept, intimacyLevel);
        
        // Temperature는 추출용으로 낮게 설정
        return openAIClient.streamRawCompletion(
            systemPrompt,
            botResponse,
            aiConfig.getExtractionTemperature() // 0.2
        );
    }
}
```

---

## 🎯 개선된 아키텍처 제안

### 전체 흐름

```
챗봇 응답 (botResponse)
  ↓
VocabularyExtractionAgent
  - 컨셉별 추출 기준 적용
  - 도메인별 어휘 분류
  - Temperature: 0.2 (정확도 우선)
  ↓ JSON
{
  "originalExpression": "검토해",
  "rootForm": "검토하다",
  "category": "BUSINESS_TERMS",
  "difficulty": 2,
  "concept": "COWORKER",
  "intimacyLevel": 1
}
  ↓
VocabularyExplanationAgent
  - 컨셉/레벨에 맞는 말투로 설명 생성
  - Temperature: 0.5 (자연스러움)
  - (선택) 말투 검증 및 리라이트
  ↓ 최종 응답
VocabularyAgentResponse
```

---

## 📝 구체적 구현 계획

### Phase 1: Agent 분리 및 기본 구조 (1주)

#### 1.1 VocabularyExtractionAgent 생성

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyExtractionAgent {
    private final OpenAIClient openAIClient;
    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper;
    
    public Mono<VocabularyExtractionResult> extract(
        String botResponse,
        String concept,
        int intimacyLevel
    ) {
        String systemPrompt = buildExtractionPrompt(concept, intimacyLevel);
        
        return openAIClient.streamRawCompletion(
            systemPrompt,
            botResponse,
            aiConfig.getExtractionTemperature()
        )
        .collectList()
        .map(this::parseExtractionResponse);
    }
    
    private String buildExtractionPrompt(String concept, int intimacyLevel) {
        // 컨셉별 추출 기준 주입
        String conceptCriteria = getConceptExtractionCriteria(concept, intimacyLevel);
        
        return String.format("""
            **역할**: 외국인 학습자에게 어려운 한국어 단어/표현을 추출하는 전문가
            
            **컨셉별 추출 기준**:
            %s
            
            **추출 규칙**:
            1. 컨셉에 맞는 어휘만 추출
            2. 난이도 2-3만 추출
            3. 동사원형 변환 필수
            
            **JSON 형식**:
            {
              "originalExpression": "원본 표현",
              "rootForm": "동사원형",
              "category": "어휘 카테고리",
              "difficulty": 2,
              "reason": "추출 이유"
            }
            """, conceptCriteria);
    }
    
    private String getConceptExtractionCriteria(String concept, int intimacyLevel) {
        return switch (concept.toUpperCase()) {
            case "FRIEND" -> """
                **FRIEND 컨셉 추출 기준**:
                - MZ세대 유행어 우선 (예: "국룰", "레알", "개좋아")
                - 속어/신조어 허용
                - 비즈니스 용어는 제외
                """;
            case "COWORKER", "BOSS" -> """
                **COWORKER/BOSS 컨셉 추출 기준**:
                - 비즈니스 용어 우선 (예: "결재", "품의", "보고")
                - 격식 표현 우선
                - MZ세대 유행어는 제외
                """;
            case "SENIOR" -> """
                **SENIOR 컨셉 추출 기준**:
                - 대학교 용어 우선 (예: "족보", "과제", "출석")
                - 격식 표현 허용
                - 속어는 제외
                """;
            default -> "일반 어휘 추출 기준";
        };
    }
}
```

#### 1.2 VocabularyExplanationAgent 생성

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyExplanationAgent {
    private final OpenAIClient openAIClient;
    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper;
    
    public Mono<VocabularyExplanationResult> generateExplanation(
        VocabularyExtractionResult extraction,
        String concept,
        int intimacyLevel
    ) {
        String systemPrompt = buildExplanationPrompt(concept, intimacyLevel);
        String userPrompt = buildUserPrompt(extraction);
        
        return openAIClient.streamRawCompletion(
            systemPrompt,
            userPrompt,
            aiConfig.getExplanationTemperature()
        )
        .collectList()
        .map(this::parseExplanationResponse);
    }
    
    private String buildExplanationPrompt(String concept, int intimacyLevel) {
        String toneGuideline = getToneGuideline(concept, intimacyLevel);
        
        return String.format("""
            **역할**: 추출된 단어를 컨셉/레벨에 맞는 말투로 설명하는 전문가
            
            **말투 규칙**:
            %s
            
            **설명 형식**:
            - context.ko: 컨셉/레벨에 맞는 말투로 100자 이내
            - context.en: 일관된 톤으로 영어 설명
            - roma: 정확한 로마자 표기
            
            **JSON 형식**:
            {
              "context": {
                "roma": "로마자 표기",
                "ko": "한국어 설명",
                "en": "English explanation"
              }
            }
            """, toneGuideline);
    }
    
    private String getToneGuideline(String concept, int intimacyLevel) {
        return switch (concept.toUpperCase()) {
            case "FRIEND" -> intimacyLevel == 3
                ? "반말 사용, 친근한 톤 (예: '~야', '~해')"
                : "부드러운 반말 사용";
            case "COWORKER", "BOSS" -> intimacyLevel == 1
                ? "격식체 사용 (예: '~습니다', '~합니다')"
                : "부드러운 존댓말 사용 (예: '~어요', '~해요')";
            case "SENIOR" -> "부드러운 존댓말 사용 (예: '~어요', '~해요')";
            default -> "표준 존댓말 사용";
        };
    }
}
```

#### 1.3 Response DTO 정의

```java
// 추출 결과
public record VocabularyExtractionResult(
    String originalExpression,  // 원본 표현
    String rootForm,             // 동사원형
    VocabularyCategory category, // 어휘 카테고리
    int difficulty,              // 난이도
    String reason                // 추출 이유
) {}

// 설명 결과
public record VocabularyExplanationResult(
    String roma,
    String ko,
    String en
) {}

// 최종 응답 (기존과 호환)
public record VocabularyAgentResponse(
    String agentType,
    List<VocabularyWord> words
) {
    public record VocabularyWord(
        String word,              // rootForm
        int difficulty,
        Context context
    ) {}
}
```

---

### Phase 2: 컨셉별 추출 기준 강화 (1주)

#### 2.1 어휘 카테고리 정의

```java
public enum VocabularyCategory {
    // 공통
    BASIC_DAILY("일상 기본 어휘", Set.of()),
    HANJA_BASED("한자어 기반 어휘", Set.of()),
    IDIOMATIC_EXPRESSION("관용 표현", Set.of()),
    
    // 컨셉별 특화
    MZ_GENERATION("MZ세대 유행어", Set.of("FRIEND", "HONEY")),
    SLANG("속어/신조어", Set.of("FRIEND", "HONEY")),
    BUSINESS_TERMS("비즈니스 용어", Set.of("COWORKER", "BOSS")),
    UNIVERSITY_TERMS("대학교 용어", Set.of("SENIOR")),
    FORMAL_EXPRESSIONS("격식 표현", Set.of("COWORKER", "BOSS", "SENIOR"));
    
    private final String description;
    private final Set<String> applicableConcepts;
    
    public boolean isApplicableTo(String concept) {
        return applicableConcepts.isEmpty() || applicableConcepts.contains(concept);
    }
}
```

#### 2.2 컨셉별 추출 우선순위

```java
private String getConceptExtractionCriteria(String concept, int intimacyLevel) {
    return switch (concept.toUpperCase()) {
        case "FRIEND" -> """
            **FRIEND 컨셉 추출 기준 (우선순위 순)**:
            1. MZ세대 유행어 (예: "국룰", "레알", "개좋아", "완전", "진짜")
            2. 속어/신조어 (예: "ㅇㅈ", "ㄱㄱ", "개웃겨")
            3. 관용 표현 (예: "~을 것 같아", "~아야 해")
            4. 한자어 기반 어휘 (예: "즉시", "참고")
            
            **제외 대상**:
            - 비즈니스 용어 (예: "결재", "품의")
            - 격식 표현 (예: "송구스럽습니다")
            """;
        case "COWORKER", "BOSS" -> """
            **COWORKER/BOSS 컨셉 추출 기준 (우선순위 순)**:
            1. 비즈니스 용어 (예: "결재", "품의", "보고", "승인")
            2. 격식 표현 (예: "송구스럽습니다", "말씀드리겠습니다")
            3. 한자어 기반 어휘 (예: "즉시", "참고", "요청")
            4. 관용 표현 (예: "~을 바탕으로", "~에 대해")
            
            **제외 대상**:
            - MZ세대 유행어 (예: "국룰", "레알")
            - 속어/신조어 (예: "개좋아", "완전")
            """;
        case "SENIOR" -> """
            **SENIOR 컨셉 추출 기준 (우선순위 순)**:
            1. 대학교 용어 (예: "족보", "과제", "출석", "수강")
            2. 격식 표현 (예: "말씀드리겠습니다")
            3. 한자어 기반 어휘 (예: "즉시", "참고")
            4. 관용 표현 (예: "~을 바탕으로")
            
            **제외 대상**:
            - MZ세대 유행어
            - 속어/신조어
            """;
        default -> "일반 어휘 추출 기준";
    };
}
```

---

### Phase 3: 말투 검증 및 리라이트 (선택, 1주)

#### 3.1 간단한 검증 (권장)

```java
@Service
public class VocabularyExplanationAgent {
    
    private boolean isToneAppropriate(
        String explanation,
        String concept,
        int intimacyLevel
    ) {
        // 규칙 기반 검증
        if (concept.equals("FRIEND") && intimacyLevel == 3) {
            // 반말 패턴 체크
            return explanation.matches(".*[해야지]야.*") 
                || explanation.contains("~해")
                || explanation.contains("~야");
        }
        
        if (concept.equals("COWORKER") || concept.equals("BOSS")) {
            // 존댓말 패턴 체크
            return explanation.matches(".*[어해]요.*")
                || explanation.contains("~습니다")
                || explanation.contains("~합니다");
        }
        
        return true;
    }
    
    private String rewriteExplanation(
        String explanation,
        String concept,
        int intimacyLevel
    ) {
        // 간단한 리라이트 규칙
        if (concept.equals("FRIEND") && intimacyLevel == 3) {
            // 존댓말 → 반말 변환
            return explanation
                .replace("~어요", "~어")
                .replace("~해요", "~해")
                .replace("~이에요", "~야");
        }
        
        return explanation;
    }
}
```

#### 3.2 LLM 기반 평가 (선택)

```java
@Service
public class VocabularyToneEvaluator {
    
    public Mono<Boolean> evaluateTone(
        String explanation,
        String concept,
        int intimacyLevel
    ) {
        String systemPrompt = """
            추출된 단어의 설명이 컨셉/레벨에 맞는 말투인지 평가하세요.
            맞으면 true, 틀리면 false를 반환하세요.
            """;
        
        String userPrompt = String.format(
            "설명: %s\n컨셉: %s\n레벨: %d",
            explanation, concept, intimacyLevel
        );
        
        return openAIClient.streamRawCompletion(systemPrompt, userPrompt)
            .collectList()
            .map(this::parseBooleanResponse);
    }
}
```

---

### Phase 4: Temperature 구분 및 설정 (즉시)

#### 4.1 AIConfig 확장

```java
@Configuration
@ConfigurationProperties(prefix = "ai")
@Getter
@Setter
public class AIConfig {
    // 기존 설정
    private String apiKey;
    private String baseUrl;
    private String model;
    
    // Agent별 설정
    private AgentConfig agents = new AgentConfig();
    
    @Getter
    @Setter
    public static class AgentConfig {
        private VocabularyConfig vocabulary = new VocabularyConfig();
    }
    
    @Getter
    @Setter
    public static class VocabularyConfig {
        private ExtractionConfig extraction = new ExtractionConfig();
        private ExplanationConfig explanation = new ExplanationConfig();
    }
    
    @Getter
    @Setter
    public static class ExtractionConfig {
        private Double temperature = 0.2;
        private Integer maxTokens = 300;
    }
    
    @Getter
    @Setter
    public static class ExplanationConfig {
        private Double temperature = 0.5;
        private Integer maxTokens = 200;
    }
}
```

#### 4.2 application.yml 설정

```yaml
ai:
  openai:
    api-key: ${OPENAI_API_KEY}
    base-url: https://api.openai.com
    model: gpt-4o-mini
    agents:
      vocabulary:
        extraction:
          temperature: 0.2  # 정확한 추출
          max-tokens: 300
        explanation:
          temperature: 0.5  # 자연스러운 설명
          max-tokens: 200
```

---

## 🔄 MultiAgentOrchestrator 수정

### 기존 코드 수정

```java
// 기존
vocabularyAgent.extractDifficultWords(actualContent)

// 변경 후
vocabularyExtractionAgent.extract(actualContent, concept, intimacyLevel)
    .flatMap(extractionResult -> 
        vocabularyExplanationAgent.generateExplanation(
            extractionResult,
            concept,
            intimacyLevel
        )
        .map(explanationResult -> 
            combineToVocabularyResponse(extractionResult, explanationResult)
        )
    )
```

---

## 📊 예상 효과

### 개선 전
- 모든 컨셉에서 동일한 기준으로 추출
- 설명 말투가 컨셉과 무관
- Temperature 고정 (0.85)

### 개선 후
- ✅ 컨셉별 맞춤 추출 (FRIEND: MZ세대 유행어, COWORKER: 비즈니스 용어)
- ✅ 설명 말투가 컨셉/레벨에 맞음
- ✅ Temperature 최적화 (추출: 0.2, 설명: 0.5)
- ✅ 도메인별 어휘 분류 (MZ세대, 비즈니스, 대학교 등)

---

## ⚠️ 주의사항 및 개선 제안

### 1. 명칭 개선

**제안:**
- `VocabularyReasoningAgent` → `VocabularyExtractionAgent`
- `StyleAgent` → `VocabularyExplanationAgent`

**이유:**
- 역할이 더 명확함
- "Reasoning"은 추론을 의미하지만 실제로는 추출
- "Style"은 말투를 의미하지만 실제로는 설명 생성

### 2. 단계적 구현 권장

**Phase 1 (필수):**
1. Agent 분리
2. 컨셉별 추출 기준
3. Temperature 구분

**Phase 2 (중요):**
4. 말투 검증 (간단한 규칙 기반)

**Phase 3 (선택):**
5. LLM 기반 평가 및 리라이트

### 3. 비용 고려

**현재:**
- VocabularyAgent 1회 호출

**개선 후:**
- ExtractionAgent 1회 + ExplanationAgent 1회 = 2회 호출
- (선택) ToneEvaluator 추가 시 = 3회 호출

**최적화 방안:**
- 캐싱: 동일한 단어/컨셉/레벨 조합은 캐시
- 배치 처리: 여러 단어를 한 번에 처리
- 임계값: 확신도가 높으면 검증 생략

### 4. 하위 호환성 유지

**기존 VocabularyAgentResponse 유지:**
```java
// 내부적으로는 분리된 Agent 사용
// 외부 인터페이스는 기존과 동일
public Mono<VocabularyAgentResponse> extractDifficultWords(String botResponse) {
    // 내부적으로 ExtractionAgent + ExplanationAgent 조합
}
```

---

## 🎯 최종 권장사항

### 즉시 구현 (Phase 1)

1. ✅ **Agent 분리**
   - VocabularyExtractionAgent
   - VocabularyExplanationAgent

2. ✅ **컨셉별 추출 기준**
   - FRIEND: MZ세대 유행어 우선
   - COWORKER/BOSS: 비즈니스 용어 우선
   - SENIOR: 대학교 용어 우선

3. ✅ **Temperature 구분**
   - Extraction: 0.2
   - Explanation: 0.5

### 중기 구현 (Phase 2)

4. ✅ **말투 검증 (규칙 기반)**
   - 간단한 패턴 매칭
   - 자동 리라이트

### 장기 검토 (Phase 3)

5. ⚠️ **LLM 기반 평가**
   - 비용/지연시간 고려
   - 필요 시에만 사용

---

## 📝 결론

**제안하신 계획은 전반적으로 매우 좋습니다!**

**개선 제안:**
1. 명칭 명확화 (Reasoning → Extraction, Style → Explanation)
2. 단계적 구현 (필수 → 중요 → 선택)
3. 비용 최적화 (캐싱, 배치 처리)
4. 하위 호환성 유지

**예상 효과:**
- 컨셉별 맞춤 어휘 추출
- 설명 말투 일관성 향상
- 추출 정확도 향상 (Temperature 최적화)

