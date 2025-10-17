package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.service.OpenAIClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 어휘 추출 Agent
 * 외국인이 이해하기 어려운 한국어 단어/표현을 최대 1개 추출
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyAgent {
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    public Mono<VocabularyAgentResponse> extractDifficultWords(String userMessage, int userLevel) {
        log.info("=== VocabularyAgent.extractDifficultWords() 호출됨 ===");
        log.info("=== VocabularyAgent 파라미터 - userMessage='{}', userLevel={} ===", userMessage, userLevel);
        
        String systemPrompt = buildVocabularyPrompt(userLevel);
        log.info("=== VocabularyAgent systemPrompt: {} ===", systemPrompt);
        
        log.info("=== VocabularyAgent OpenAI API 호출 시작 ===");
        return openAIClient.streamRawCompletion(systemPrompt, userMessage)
            .doOnError(error -> log.error("VocabularyAgent 스트림 오류", error))
            .collectList()
            .doOnError(error -> log.error("VocabularyAgent collectList 오류", error))
            .map(this::parseVocabularyResponse)
            .doOnSuccess(response -> log.info("VocabularyAgent 파싱 완료: {} 단어 추출", response.words().size()))
            .doOnError(error -> log.error("VocabularyAgent 파싱 오류", error));
    }
    
    private String buildVocabularyPrompt(int userLevel) {
        return """
            **ver 0.4**

            **역할 설명:**

            너는 AI 챗봇이 생성한 한국어 응답 문장을 정밀하게 분석하여, 외국인 한국어 학습자에게 난이도가 높은 어휘를 추출하고 친절하게 설명하는 전문가 역할을 수행할거야

            단순히 단어를 추출하는 것을 넘어, 한국어 어휘의 난이도와 사용 맥락에 대한 깊은 이해를 바탕으로 학습 자료의 완성도를 높이는 전문가인거야

            **입력 정보:**

            - 학습자 레벨: {userLevel}
            - 분석 대상 문장 (AI 챗봇 응답): "{userMessage}"

            **난이도 정의 (상황 독립적):**

            - 1 (초급): 일상 기본 어휘, 단순 어미, 기초 동사/명사. (예: 오늘, 하다, 좋다)
            - 2 (중급): 한자어 기반의 일반 어휘, 복합 동사, 관용 표현, 일반적인 사회/업무 용어. (예: 참고, 요청, 말씀, ~시죠)
            - 3 (고급): 복잡한 한자 숙어, 신조어/속어, 고도의 완곡/문어체 표현, 비즈니스 전문 용어. (예: 결재, 품의, 송구스럽습니다, 국룰)

            **어휘(단어) 추출 기준:**

            1. 반드시 "{userMessage}"에 포함된 단어만 추출할 것
            2. 어휘 난이도가 '중급(2단계)' 이상인 단어/표현만 추출할 것 (난이도 1인 단어 추출 절대 금지.)
            3. 항상 1개의 단어만 반환할 것
            4. `context` 필드의 `ko`와 `en` 설명은 부드럽고 친근한 톤앤매너를 사용하여 학습자에게 친절하게 설명할 것
            5. `context` 필드의 `ko` 설명은 100자 이내로 작성할 것
            6. 어려운 어휘가 없을 경우, 빈 객체 (`{}`)를 반환할 것

            **JSON 형식(단어 있을 경우):**

            다음 JSON 형식으로 정확히 답변하세요:

            [
            {
            "word": "추출된 단어",
            "difficulty": 2,
            "context": {
            "roma": "어휘의 정확한 로마자 표기 (예: Gyeoljae)",
            "ko": "새로운 톤앤매너로 100자 이내 작성된 한국어 설명이에요.",
            "en": "English explanation written in a friendly and consistent tone."
            }
            }
            ]

            **주의사항:**

            - 항상 1개의 단어만 반환할 것
            - 어려운 어휘가 없으면 빈 배열 ({})을 반환할 것
            - JSON 형식 외의 텍스트는 출력하지 말 것
            - `context`의 `ko`, `en` 필드는 부드럽고 친근한 톤앤매너를 지켜서 작성할 것
            - `context` 필드의 `ko` 설명은 100자 이내로 작성할 것

            **예시 시나리오:**

            **[시나리오 1: 어려운 어휘가 있는 경우]**
            입력 정보: 

            {
            "userMessage": "보고서는 내일 오전까지 결재 올리겠습니다."
            }

            응답 형식:

            [
            {
            "word": "결재",
            "difficulty": 3,
            "context": {
            "roma": "Gyeoljae",
            "ko": "결재는 직장 상사에게 서류나 계획을 보여드리고 승인받는 것을 말해요. 격식 있는 업무 상황에서 주로 쓰는 중요한 단어예요.",
            "en": "Gyeoljae means asking your boss for official permission or approval on a document or plan. It is an important word mainly used in formal business settings."
            }
            }
            ]

            **[시나리오 2: 어려운 어휘가 있는 경우]**
            입력 정보:

            {
            "userMessage": "혹시 문제가 있다면 즉시 말씀해 주시면 좋을 것 같아요."
            }

            응답 형식:

            [
            {
            "word": "즉시",
            "difficulty": 2,
            "context": {
            "roma": "Jeuksi",
            "ko": "즉시는 '바로 지금'이라는 뜻을 가진 한자어예요. 공식적인 자리나 업무에서 '빨리'라는 의미를 강조할 때 사용하는 경향이 있어요.",
            "en": "Jeuksi is a Sino-Korean word meaning 'right now' or 'immediately.' People tend to use it in formal or business settings to emphasize the urgency of doing something quickly."
            }
            }
            ]

            **[시나리오 3: 어려운 어휘가 있는 경우 (새로운 톤앤매너 예시)]**
            입력 정보:

            {
            "userMessage": "그 제안에 대해 제가 잠시 검토해 보도록 하겠습니다."
            }

            응답 형식:

            [
            {
            "word": "검토",
            "difficulty": 2,
            "context": {
            "roma": "Geomto",
            "ko": "검토는 어떤 내용이나 계획에 대해 '자세하게 살펴보고 문제가 없는지 확인한다'는 뜻이에요. 회사에서 문서를 처리할 때 자주 사용하는 표현이라고 해요.",
            "en": "Geomto means 'to review in detail and check for any problems' with content or a plan. People say it is a phrase frequently used when handling documents at work."
            }
            }
            ]

            **[시나리오 4: 어려운 어휘가 없는 경우]**
            입력 정보:

            {
            "userMessage": "오늘 몇 시에 퇴근하세요?"
            }

            응답 형식:

            {}
            """;
    }
    
    private VocabularyAgentResponse parseVocabularyResponse(List<String> chunks) {
        try {
            // OpenAI 스트림에서 실제 content만 추출
            StringBuilder contentBuilder = new StringBuilder();
            for (String chunk : chunks) {
                try {
                    JsonNode chunkJson = objectMapper.readTree(chunk);
                    if (chunkJson.has("choices") && chunkJson.get("choices").isArray() && chunkJson.get("choices").size() > 0) {
                        JsonNode choice = chunkJson.get("choices").get(0);
                        if (choice.has("delta") && choice.get("delta").has("content")) {
                            String content = choice.get("delta").get("content").asText();
                            contentBuilder.append(content);
                        }
                    }
                } catch (Exception e) {
                    log.debug("VocabularyAgent 청크 파싱 실패 (무시): {}", e.getMessage());
                }
            }
            
            String fullResponse = contentBuilder.toString();
            
            if (fullResponse.trim().isEmpty()) {
                return new VocabularyAgentResponse("vocabulary", List.of());
            }
            
            JsonNode json;
            try {
                json = objectMapper.readTree(fullResponse);
            } catch (Exception e) {
                log.warn("VocabularyAgent JSON 파싱 실패: {}", e.getMessage());
                return new VocabularyAgentResponse("vocabulary", List.of());
            }
            
            List<VocabularyAgentResponse.VocabularyWord> words = new ArrayList<>();
            
            // 빈 객체인 경우 처리
            if (json.isEmpty() || (json.has("words") && json.get("words").isArray() && json.get("words").size() == 0)) {
                return new VocabularyAgentResponse("vocabulary", words);
            }
            
            // 배열 형태로 직접 파싱 (새로운 JSON 형식)
            if (json.isArray()) {
                for (JsonNode wordNode : json) {
                    String word = wordNode.has("word") ? wordNode.get("word").asText() : "";
                    int difficulty = wordNode.has("difficulty") ? wordNode.get("difficulty").asInt() : 1;
                    
                    VocabularyAgentResponse.Context context = null;
                    if (wordNode.has("context") && wordNode.get("context").isObject()) {
                        JsonNode contextNode = wordNode.get("context");
                        String roma = contextNode.has("roma") ? contextNode.get("roma").asText() : "";
                        String ko = contextNode.has("ko") ? contextNode.get("ko").asText() : "";
                        String en = contextNode.has("en") ? contextNode.get("en").asText() : "";
                        context = new VocabularyAgentResponse.Context(roma, ko, en);
                    }
                    
                    if (!word.isEmpty() && context != null) {
                        words.add(new VocabularyAgentResponse.VocabularyWord(word, difficulty, context));
                    }
                }
            }
            // 기존 words 배열 형태도 지원 (하위 호환성)
            else if (json.has("words") && json.get("words").isArray()) {
                for (JsonNode wordNode : json.get("words")) {
                    String word = wordNode.has("word") ? wordNode.get("word").asText() : "";
                    int difficulty = wordNode.has("difficulty") ? wordNode.get("difficulty").asInt() : 1;
                    
                    VocabularyAgentResponse.Context context = null;
                    if (wordNode.has("context") && wordNode.get("context").isObject()) {
                        JsonNode contextNode = wordNode.get("context");
                        String roma = contextNode.has("roma") ? contextNode.get("roma").asText() : "";
                        String ko = contextNode.has("ko") ? contextNode.get("ko").asText() : "";
                        String en = contextNode.has("en") ? contextNode.get("en").asText() : "";
                        context = new VocabularyAgentResponse.Context(roma, ko, en);
                    }
                    
                    if (!word.isEmpty() && context != null) {
                        words.add(new VocabularyAgentResponse.VocabularyWord(word, difficulty, context));
                    }
                }
            }
            
            return new VocabularyAgentResponse("vocabulary", words);
        } catch (Exception e) {
            log.error("VocabularyAgent 응답 파싱 실패", e);
            return new VocabularyAgentResponse("vocabulary", List.of());
        }
    }
}
