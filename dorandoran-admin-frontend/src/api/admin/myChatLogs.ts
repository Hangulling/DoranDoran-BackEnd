import api from '../api'
import type {
  ChatLogListPageResponse,
  ChatLogSearchRequest,
  ChatroomOption,
  IntimacyLevelOption,
  MessageTimelinePageResponse,
} from '../../types/chatLog'

const BASE_PATH = '/api/admin/chat-logs'

/**
 * 채팅룸 옵션 조회 (드롭다운용)
 */
export const getChatroomOptions = async (): Promise<ChatroomOption[]> => {
  const response = await api.get<ChatroomOption[]>(`${BASE_PATH}/chatrooms`)
  return response.data
}

/**
 * 친밀도 레벨 옵션 조회 (드롭다운용)
 */
export const getIntimacyLevelOptions = async (): Promise<IntimacyLevelOption[]> => {
  const response = await api.get<IntimacyLevelOption[]>(`${BASE_PATH}/intimacy-levels`)
  return response.data
}

/**
 * 채팅 로그 검색
 */
export const searchChatLogs = async (
  params: ChatLogSearchRequest
): Promise<ChatLogListPageResponse> => {
  const searchParams = new URLSearchParams()

  // 필수 파라미터
  searchParams.set('startDate', params.startDate)

  // 선택 파라미터
  if (params.endDate) searchParams.set('endDate', params.endDate)
  if (params.chatroomId) searchParams.set('chatroomId', params.chatroomId)
  if (params.intimacyLevel !== undefined) {
    searchParams.set('intimacyLevel', String(params.intimacyLevel))
  }

  // 페이징
  searchParams.set('page', String(params.page ?? 0))
  searchParams.set('size', String(params.size ?? 20))

  const response = await api.get<ChatLogListPageResponse>(
    `${BASE_PATH}/search?${searchParams.toString()}`
  )
  return response.data
}

/**
 * 채팅 상세 타임라인 조회
 */
export const getChatLogTimeline = async (
  chatroomId: string,
  page: number = 0,
  size: number = 50
): Promise<MessageTimelinePageResponse> => {
  const searchParams = new URLSearchParams()
  searchParams.set('page', String(page))
  searchParams.set('size', String(size))

  const response = await api.get<MessageTimelinePageResponse>(
    `${BASE_PATH}/${chatroomId}/timeline?${searchParams.toString()}`
  )
  return response.data
}