import api from '../api'
import type {
  ManagementQueuePageResponse,
  ManagementQueueResponse,
  ManagementQueueRequest,
  ManagementQueueCountResponse,
  BatchCompleteRequest,
  BatchDeleteRequest,
  BatchOperationResponse,
  CompleteRequest,
  QueueType,
  QueueStatus,
} from '../../types/managementQueue'
import type { AuditLogPageResponse } from '../../types/auditLog'

const BASE_PATH = '/api/admin/management-queue'

/**
 * 관리 필요 내역 등록
 */
export const createManagementQueue = async (
  request: ManagementQueueRequest
): Promise<ManagementQueueResponse> => {
  const response = await api.post<ManagementQueueResponse>(BASE_PATH, request)
  return response.data
}

/**
 * 관리 필요 내역 목록 조회
 */
export const getManagementQueueList = async (
  queueType?: QueueType,
  status?: QueueStatus,
  page: number = 0,
  size: number = 20
): Promise<ManagementQueuePageResponse> => {
  const searchParams = new URLSearchParams()

  if (queueType) searchParams.set('queueType', queueType)
  if (status) searchParams.set('status', status)
  searchParams.set('page', String(page))
  searchParams.set('size', String(size))

  const response = await api.get<ManagementQueuePageResponse>(
    `${BASE_PATH}?${searchParams.toString()}`
  )
  return response.data
}

/**
 * 관리 필요 내역 단건 조회
 */
export const getManagementQueue = async (id: string): Promise<ManagementQueueResponse> => {
  const response = await api.get<ManagementQueueResponse>(`${BASE_PATH}/${id}`)
  return response.data
}

/**
 * 관리 필요 내역 메모 수정
 */
export const updateManagementQueue = async (
  id: string,
  memo: string
): Promise<ManagementQueueResponse> => {
  const response = await api.put<ManagementQueueResponse>(`${BASE_PATH}/${id}`, { memo })
  return response.data
}

/**
 * 관리 필요 내역 단건 삭제
 */
export const deleteManagementQueue = async (id: string): Promise<void> => {
  await api.delete(`${BASE_PATH}/${id}`)
}

/**
 * 관리 필요 내역 단건 처리 완료
 */
export const completeManagementQueue = async (
  id: string,
  request: CompleteRequest
): Promise<ManagementQueueResponse> => {
  const response = await api.patch<ManagementQueueResponse>(
    `${BASE_PATH}/${id}/complete`,
    request
  )
  return response.data
}

/**
 * 타입별 카운트 조회
 */
export const getManagementQueueCount = async (
  status: QueueStatus = 'PENDING'
): Promise<ManagementQueueCountResponse> => {
  const searchParams = new URLSearchParams()
  searchParams.set('status', status)

  const response = await api.get<ManagementQueueCountResponse>(
    `${BASE_PATH}/count?${searchParams.toString()}`
  )
  return response.data
}

/**
 * 일괄 처리 완료
 */
export const batchCompleteManagementQueue = async (
  request: BatchCompleteRequest
): Promise<BatchOperationResponse> => {
  const response = await api.patch<BatchOperationResponse>(
    `${BASE_PATH}/complete-batch`,
    request
  )
  return response.data
}

/**
 * 일괄 삭제
 */
export const batchDeleteManagementQueue = async (
  request: BatchDeleteRequest
): Promise<BatchOperationResponse> => {
  const response = await api.delete<BatchOperationResponse>(`${BASE_PATH}/batch`, {
    data: request,
  })
  return response.data
}

/**
 * 감사 로그 조회
 */
export const getAuditLogs = async (
  queueType?: QueueType,
  adminName?: string,
  startDate?: string,
  endDate?: string,
  page: number = 0,
  size: number = 20
): Promise<AuditLogPageResponse> => {
  const searchParams = new URLSearchParams()

  if (queueType) searchParams.set('queueType', queueType)
  if (adminName) searchParams.set('adminName', adminName)
  if (startDate) searchParams.set('startDate', startDate)
  if (endDate) searchParams.set('endDate', endDate)
  searchParams.set('page', String(page))
  searchParams.set('size', String(size))

  const response = await api.get<AuditLogPageResponse>(
    `${BASE_PATH}/audit-logs?${searchParams.toString()}`
  )
  return response.data
}