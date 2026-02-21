import { decodeJwt } from '../utils/authToken'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export interface AdminUser {
  id: string
  name: string
  email: string
  role?: string
}

export interface ChatroomOption {
  id: string
  name: string
  concept: string
  userEmailSnapshot: string
}

/**
 * Admin 사용자 정보 조회
 * 토큰에서 사용자 ID를 추출하여 사용자 정보를 가져옵니다.
 */
export async function getAdminUser(): Promise<AdminUser> {
  const token = sessionStorage.getItem('accessToken')
  if (!token) {
    throw new Error('No access token found')
  }

  // 토큰에서 사용자 ID 추출
  const decoded = decodeJwt(token)
  const userId = (decoded?.sub as string) || (decoded?.userId as string) || (decoded?.id as string)

  if (!userId) {
    throw new Error('User ID not found in token')
  }

  // 사용자 정보 조회
  const response = await fetch(`${API_BASE_URL}/api/users/${userId}`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error(`Failed to fetch admin user: ${response.statusText}`)
  }

  const userData = await response.json()
  
  return {
    id: userData.id,
    name: userData.name,
    email: userData.email,
    role: userData.role,
  }
}

/**
 * 채팅룸 옵션 조회
 */
export async function getChatroomOptions(): Promise<ChatroomOption[]> {
  const token = sessionStorage.getItem('accessToken')
  if (!token) {
    throw new Error('No access token found')
  }

  const response = await fetch(`${API_BASE_URL}/api/admin/chat-logs/chatrooms`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error(`Failed to fetch chatroom options: ${response.statusText}`)
  }

  return response.json()
}
