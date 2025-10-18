// 컴포넌트용
export interface ChatRoom {
  roomId: number
  roomName: string
  avatar: string
  message: string
  intimacy: number
}

export interface Message {
  id: number
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
}

// api
export interface CreateChatroomPayload {
  userId?: string // (JWT에서 자동추출)
  chatbotId?: string
  name?: string
  concept: string // FRIEND, HONEY, COWORKER, SENIOR
  intimacyLevel?: number
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
  senderId: string
  senderType: 'user' | 'ai'
  content: string
  contentType: string
  createdAt: string
}

export interface UpdateIntimacyPayload {
  intimacyLevel: number // 1-3
}
