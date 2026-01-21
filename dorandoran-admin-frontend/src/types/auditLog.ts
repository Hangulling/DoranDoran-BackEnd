import type {
  QueueType,
  QueueStatus,
  RequestData,
  ResultData,
  PageInfo
} from './managementQueue'

// 감사 로그 응답
// ManagementQueueResponse와 동일한 구조이지만, 감사 로그 관점으로 사용
export interface AuditLogResponse {
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

// 페이징된 감사 로그
export interface AuditLogPageResponse {
  content: AuditLogResponse[]
  page: PageInfo
}

// 감사 로그 검색 요청
export interface AuditLogSearchRequest {
  queueType?: QueueType
  adminName?: string
  startDate: string  // ISO-8601 형식 (yyyy-MM-ddTHH:mm:ss)
  endDate: string    // ISO-8601 형식 (yyyy-MM-ddTHH:mm:ss)
  page?: number
  size?: number
}