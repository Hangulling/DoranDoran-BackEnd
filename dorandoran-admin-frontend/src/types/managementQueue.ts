// 큐 타입
export type QueueType = 'CORRECTION' | 'DELETION'

// 큐 상태
export type QueueStatus = 'PENDING' | 'COMPLETED'

// 관리 필요 내역 응답
export interface ManagementQueueResponse {
  id: string
  queueType: QueueType
  status: QueueStatus
  requestData: RequestData
  resultData?: ResultData
  adminName: string
  adminIp: string
  createdAt: string
  updatedAt?: string
  completedAt?: string
  errorMessage?: string
}

// requestData 구조
export interface RequestData {
  items: QueueItem[]
  memo?: string
  conversationId?: string
  chatroomId?: string
  messageId?: string
}

// 큐 항목
export interface QueueItem {
  type: 'intimacy' | 'conversation' | 'voca'  // 실제 아이템 타입
  messageId: string
  content?: string
  detectedLevel?: number
  correctedSentence?: string
  corrections?: string
  feedback?: {
    ko: string
    en: string
  }
  word?: string
  difficulty?: number
  context?: string
}

// resultData 구조
export interface ResultData {
  processedBy: string
  processedAt: string
  action: string
  note: string
}

// 타입별 카운트 응답
export interface ManagementQueueCountResponse {
  intimacyCount: number
  conversationCount: number
  vocaCount: number
  totalPendingCount: number
}

// 페이지 정보
export interface PageInfo {
  number: number
  size: number
  totalPages: number
  totalElements: number
}

// 페이징된 관리 필요 내역
export interface ManagementQueuePageResponse {
  content: ManagementQueueResponse[]
  page: PageInfo
}

// === 요청 DTO ===

// 관리 필요 내역 등록 요청
export interface ManagementQueueRequest {
  queueType: QueueType
  requestData: RequestData
}

// 관리 필요 내역 수정 요청
export interface ManagementQueueUpdateRequest {
  memo: string
}

// 일괄 처리 완료 요청
export interface BatchCompleteRequest {
  ids: string[]
  note: string
}

// 일괄 삭제 요청
export interface BatchDeleteRequest {
  ids: string[]
}

// 단건 처리 완료 요청
export interface CompleteRequest {
  note: string
}

// 일괄 처리 응답
export interface BatchOperationResponse {
  completedCount: number
  totalRequested: number
  message: string
}