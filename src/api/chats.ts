import type {
  ApiChatRoom,
  ApiMessage,
  ChatRoomListParams,
  CreateChatroomPayload,
  LastInteraction,
  PagedApiMessageResponse,
  SendMessagePayload,
  UpdateIntimacyPayload,
} from '../types/chat'
import api from './api'
import { CHAT_ENDPOINTS } from './endpoints'

// 채팅방 생성(또는 기존 채팅방 조회)
export async function createChatRoom(data: CreateChatroomPayload): Promise<ApiChatRoom> {
  const res = await api.post(CHAT_ENDPOINTS.CREATE, data)
  return res.data
}

// 채팅방(목록) 조회
export async function chatRoomList(
  page?: number,
  size?: number,
  userId?: string
): Promise<ChatRoomListParams> {
  const url = CHAT_ENDPOINTS.CHATROOM_LIST(userId, page ?? 0, size ?? 20)
  const response = await api.get(url)
  return response.data
}

// 채팅방 단건 조회
export async function getChatRoom(chatroomId: string, userId?: string): Promise<ApiChatRoom> {
  const params = userId ? { userId } : {}
  const res = await api.get(CHAT_ENDPOINTS.GET_CHATROOM(chatroomId), { params })
  return res.data
}

// 채팅방 목록 (최대 4개) 조회
export async function getChatRoomListLimited(userId?: string): Promise<ApiChatRoom[]> {
  const params = userId ? { userId } : {}
  const res = await api.get(CHAT_ENDPOINTS.CHATROOM_LIST_LIMITED, { params })
  return res.data
}

// 메시지 목록 조회
export async function getMessages(
  chatroomId: string,
  params: {
    userId?: string
    page?: number
    size?: number
  } = {}
): Promise<PagedApiMessageResponse> {
  const res = await api.get<PagedApiMessageResponse>(CHAT_ENDPOINTS.MESSAGES_LIST(chatroomId), {
    params,
  })
  return res.data
}

// 메시지 전송
export async function sendMessage(
  chatroomId: string,
  data: SendMessagePayload
): Promise<ApiMessage> {
  const res = await api.post(CHAT_ENDPOINTS.SEND_MESSAGE(chatroomId), data)
  return res.data
}

// 친밀도 업데이트
export async function updateIntimacy(
  chatroomId: string,
  payload: UpdateIntimacyPayload
): Promise<ApiChatRoom> {
  const response = await api.patch(CHAT_ENDPOINTS.UPDATE_INTIMACY_LEVEL(chatroomId), payload)
  return response.data
}

// 채팅방 나가기 (소프트 딜리트)
export async function leaveChatroom(chatroomId: string, userId: string): Promise<void> {
  await api.post(CHAT_ENDPOINTS.LEAVE_CHATROOM(chatroomId, userId))
}

// SSE(실시간 메시지 스트림)
export function getSseUrl(chatroomId: string, userId?: string): string {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const url = new URL(`${baseUrl}${CHAT_ENDPOINTS.MESSAGE_STREAM(chatroomId)}`)

  if (userId) {
    url.searchParams.append('userId', userId)
  }

  return url.toString()
}

// 마지막 채팅 시간
export async function getLastInteractions(userId: string): Promise<LastInteraction[]> {
  const url = CHAT_ENDPOINTS.LAST_INTERACTIONS(userId)
  const res = await api.get<LastInteraction[]>(url)
  return res.data
}

// 테스트
// 마지막 채팅 시간 (테스트 모델 필터)
export async function getLastTestInteractions(
  userId: string,
  testModel: 'a' | 'b' | 'c'
): Promise<LastInteraction[]> {
  const url = CHAT_ENDPOINTS.LAST_INTERACTIONS_TEST(userId, testModel)
  const res = await api.get<LastInteraction[]>(url)
  return res.data
}

// 테스트 채팅방 생성(또는 기존 채팅방 조회)
export async function createTestChatRoom(
  data: CreateChatroomPayload & { testModel: 'a' | 'b' | 'c' }
): Promise<ApiChatRoom> {
  const res = await api.post(CHAT_ENDPOINTS.CREATE, data)
  return res.data
}
