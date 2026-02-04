export interface ConversationCompleteContent {
  content: string // 실제 메시지 내용
}

export interface IntimacyAnalysisData {
  messageId: string // 임의 저장한 id
  corrections: string // 교정 문장 설명
  feedback: {
    ko: string // 한국어 피드백
    en: string // 영어 피드백
  }
  correctedSentence: string // 교정된 문장
  detectedLevel?: number // 감지 난이도 또는 레벨
}

export interface VocabularyWordContext {
  roma: string // 단어 발음(로마자)
  ko: string // 한국어 설명
  en: string // 영어 설명
}

export interface VocabularyWord {
  difficulty: number // 단어 난이도
  word: string // 단어
  context: VocabularyWordContext // 단어 문맥 정보
}

export interface VocabularyExtractedData {
  words: VocabularyWord[] // 단어 목록과 세부 문맥 포함
}

export interface VocabularyTranslatedData {
  translations: Record<string, string>
}

export interface ConversationCompleteData {
  messageId: string
  content: string // 내부 JSON 문자열
}

export interface AggregatedCompleteData {
  summary: string
}

export interface AiInfoData {
  version: string
  model: string
}

export interface AgentErrorData {
  message: string
  code: number
}

export interface ConversationErrorData {
  error: string
}

export interface GreetingBotMessageData {
  messageId: string
  content: string
  senderType: 'bot'
  timestamp: string
}

export interface GreetingGuideMessageData {
  messageId: string
  content: string
  senderType: 'system'
  timestamp: string
}

export interface EventDataMap {
  // 기존 이벤트
  conversation_complete: ConversationCompleteData
  intimacy_analysis: IntimacyAnalysisData | null
  vocabulary_extracted: VocabularyExtractedData | null
  aggregated_complete: AggregatedCompleteData
  ai_info: AiInfoData
  agent_error: AgentErrorData
  conversation_error: ConversationErrorData
  message: unknown // 기본 메시지 이벤트

  // 새로운 그리팅 이벤트
  greeting_bot_message: GreetingBotMessageData
  greeting_guide_message: GreetingGuideMessageData
}
