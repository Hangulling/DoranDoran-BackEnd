import type { IntimacyAnalysisData, VocabularyExtractedData, VocabularyWord } from './sseEvents'

// 컴포넌트용
export interface ChatRoom {
  roomId: number
  roomName: string
  avatar: string
  message: string
  intimacy: number
}

export interface Message {
  id: string
  text: string
  isSender: boolean
  avatarUrl?: string
  variant?: 'basic' | 'second' | 'sender'
  showIcon?: boolean
  explanation?: {
    word: string
    pronunciation: string
    selectedTab: 'Kor' | 'Eng'
    descriptionByTab: {
      Kor: string
      Eng: string
    }
  }
  correction?: IntimacyAnalysisData | null
  vocabulary?: VocabularyWord[]
}

// api
export interface CreateChatroomPayload {
  userId?: string // (JWT에서 자동추출)
  chatbotId?: string
  name?: string
  concept: string // FRIEND, HONEY, COWORKER, SENIOR
  intimacyLevel?: number
  testModel?: 'a' | 'b' | 'c' // 테스트 모델
}

export interface ApiChatRoom {
  id: string
  userId: string
  chatbotId?: string
  name: string
  description: string
  concept: string
  intimacyLevel: number
  lastMessageAt: string
  lastMessageId: string
  isArchived: boolean
  isDeleted: boolean
  createdAt: string
  updatedAt: string
}

// 페이징 정보 타입
export interface Pageable {
  sort: {
    sorted: boolean
    unsorted: boolean
  }
  pageNumber: number
  pageSize: number
}

export interface ChatRoomListParams {
  content: ApiChatRoom[]
  pageable: Pageable
  totalElements: number
  totalPages: number
  last: boolean
  first: boolean
  numberOfElements: number
}

export interface MessageListResponse {
  content: Message[]
  pageable: Pageable
  totalElements: number
  totalPages: number
  last: boolean
  first: boolean
  numberOfElements: number
}

export interface SendMessagePayload {
  senderType?: 'user' | 'bot' | 'system' // 기본값 user
  content: string
  contentType?: string // text, code, system 기본값 text
}

export interface ApiMessage {
  id: string
  chatroomId: string
  senderId: string | null
  senderType: 'user' | 'bot' | 'system'
  content: string
  contentType: string
  sequenceNumber: number
  isEdited: boolean
  isDeleted: boolean
  createdAt: string
  metadata: ApiMessageMetadata | null // metadata 필드 추가
}

export interface PagedApiMessageResponse {
  content: ApiMessage[]
  pageable: Pageable
  totalElements: number
  totalPages: number
  last: boolean
  first: boolean
  numberOfElements: number
}

export interface UpdateIntimacyPayload {
  intimacyLevel: number // 1-3
}

// metadata.userMessageAnalysis.intimacy
export interface ApiIntimacyData {
  detectedLevel: number
  correctedSentence: string
  feedback: {
    ko: string
    en: string
  }
  corrections: string
}

// metadata.userMessageAnalysis
export interface UserMessageAnalysis {
  userMessageId: string
  intimacy: ApiIntimacyData
}

// metadata.botResponseAnalysis
export interface BotResponseAnalysis {
  vocabulary: VocabularyExtractedData
}

// metadata
export interface ApiMessageMetadata {
  userMessageAnalysis: UserMessageAnalysis | null
  botResponseAnalysis: BotResponseAnalysis | null
}

export interface LastInteraction {
  chatbotId: string
  lastRoomId: string
  lastInteractionAt: string
  chatbotName: string
}
