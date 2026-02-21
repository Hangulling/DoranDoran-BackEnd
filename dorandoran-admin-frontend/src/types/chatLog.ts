// 채팅 로그 리스트 응답
export interface ChatLogListResponse {
  chatroomId: string
  chatroomName: string
  concept: string
  intimacyLevel: number
  lastMessageAt: string
  messageCount: number
  userEmail: string
}

// 채팅 로그 검색 요청
export interface ChatLogSearchRequest {
  startDate: string  // LocalDate (yyyy-MM-dd)
  endDate?: string
  chatroomId?: string
  intimacyLevel?: number
  page?: number
  size?: number
}

// 메시지 타임라인 응답
export interface MessageTimelineResponse {
  messageId: string
  content: string
  senderType: string  // USER, CHATBOT, SYSTEM
  sequenceNumber: number
  turnNumber: number
  sourceCreatedAt: string
  tokenCount?: number
  processingTimeMs?: number
  agentResults?: AgentResultResponse
}

// Agent 결과 응답
export interface AgentResultResponse {
  intimacy?: IntimacyResult
  conversation?: ConversationResult
  vocabulary?: VocabularyResult
}

export interface IntimacyResult {
  detectedLevel: number
  correctedSentence: string
  corrections: string
  feedback: {
    ko: string
    en: string
  }
}

export interface ConversationResult {
  content: string
}

export interface VocabularyResult {
  words: VocabularyWord[]
}

export interface VocabularyWord {
  word: string
  difficulty: number
  context: string
}

// 채팅룸 옵션 (드롭다운용)
export interface ChatroomOption {
  id: string
  name: string
  concept: string
  userEmail: string
}

// 친밀도 레벨 옵션 (드롭다운용)
export interface IntimacyLevelOption {
  level: number
  description: string
}

// 페이지 정보
export interface PageInfo {
  number: number
  size: number
  totalPages: number
  totalElements: number
}

// 페이징된 채팅 로그 리스트
export interface ChatLogListPageResponse {
  content: ChatLogListResponse[]
  page: PageInfo
}

// 페이징된 메시지 타임라인
export interface MessageTimelinePageResponse {
  content: MessageTimelineResponse[]
  page: PageInfo
}