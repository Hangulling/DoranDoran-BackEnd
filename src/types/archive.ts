export type Room = 'Friend' | 'Honey' | 'Coworker' | 'Senior'

export interface ExpressionItem {
  id: string
  chatRoom: Room
  text: string
  intimacy: number
  ttsUrl?: string
  savedAt?: string
}

// --- 챗봇 타입 ---
export type BotType = 'friend' | 'honey' | 'coworker' | 'senior'

// --- 번역 정보 ---
export interface Translation {
  english: string
  pronunciation?: string
}

// --- 어휘 정보 ---
export interface Vocabulary {
  word: string
  pronunciation: string
  explanation: string
  korExplanation: string
}

// --- AI 피드백 ---
export interface AiResponse {
  intimacyLevel?: string // 예: "Close" | "Casual"
  description?: string
  translation?: Translation
  vocabulary?: Vocabulary[]
  corrections?: string[]
}

// --- 북마크 저장 요청 ---
export interface BookmarkRequest {
  messageId: string // 메시지 ID (UUID)
  chatroomId: string // 채팅방 ID (UUID)
  chatbotId: string // 챗봇 ID (UUID)
  content: string // 원본 표현
  aiResponse: AiResponse // AI 피드백
  botType?: BotType // 챗봇 타입
}

// --- 북마크 응답 ---
export interface BookmarkResponse {
  id: string
  messageId: string
  chatroomId: string
  chatroomName?: string
  content: string
  aiResponse: AiResponse
  botType: BotType
  correctedContent?: string
  createdAt: string
  ttsUrl?: string
  savedAt?: string
}

// --- 커서 기반 조회 응답 ---
export interface CursorPage<T> {
  content: T[]
  totalElements: number
  numberOfElements: number
  last: boolean
  first: boolean
  empty: boolean
}

export const BOT_TO_ROOM: Record<BotType, Room> = {
  friend: 'Friend',
  honey: 'Honey',
  coworker: 'Coworker',
  senior: 'Senior',
}

export const ROOM_TO_BOT: Record<Room, BotType> = {
  Friend: 'friend',
  Honey: 'honey',
  Coworker: 'coworker',
  Senior: 'senior',
}

export interface BookmarkAiResponse {
  intimacyLevel: string
  correctedContent: string | null
  explanation: string
  koreanPronunciation: string
  englishTranslation: string
  situation: string
}

export interface BookmarkByRoomResponse {
  id: string
  messageId: string
  chatroomId: string
  chatroomName: string
  content: string
  correctedContent: string | null
  aiResponse: BookmarkAiResponse
  botType: BotType
  createdAt: string
}
