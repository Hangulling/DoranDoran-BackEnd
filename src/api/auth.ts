import axios from 'axios'
import type {
  LoginRequest,
  LoginResponse,
  OAuthLoginRequest,
  OAuthLoginResponse,
  VerificationRequest,
} from '../types/auth'
import api, { publicApi } from './api'
import { AUTH_ENDPOINTS, USER_ENDPOINTS } from './endpoints'

let currentUserId: string | null = null

export async function login(data: LoginRequest) {
  const res = await api.post<LoginResponse>(AUTH_ENDPOINTS.LOGIN, data)
  const { data: resData } = res.data
  const { accessToken, refreshToken } = resData

  if (accessToken) {
    sessionStorage.setItem('accessToken', accessToken)
  }

  if (refreshToken) {
    sessionStorage.setItem('refreshToken', refreshToken)
  }
  return res.data
}

export async function oauthLogin(data: OAuthLoginRequest) {
  const res = await publicApi.post<OAuthLoginResponse>(AUTH_ENDPOINTS.OAUTH_LOGIN, data)
  const { data: resData } = res.data
  const { accessToken, refreshToken } = resData

  if (accessToken) {
    sessionStorage.setItem('accessToken', accessToken)
  }

  if (refreshToken) {
    sessionStorage.setItem('refreshToken', refreshToken)
  }
  return res.data
}

export async function logout() {
  sessionStorage.setItem('session:manualLogout', '1')

  try {
    const res = await api.post(AUTH_ENDPOINTS.LOGOUT)
    if (import.meta.env.DEV) {
      console.log('🔒 로그아웃 성공:', res.data.message)
    }
    return res.data
  } catch (error) {
    console.error('🚨 로그아웃 요청 중 오류 발생:', error)
    throw error
  } finally {
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
    sessionStorage.removeItem('currentUserId')
    currentUserId = null
    try {
      sessionStorage.setItem('session:logout', String(Date.now()))
    } catch {
      console.warn('Failed to set logout flag')
    }
    setTimeout(() => localStorage.removeItem('session:manualLogout'), 1500)
  }
}

export async function checkEmailExists(email: string): Promise<boolean> {
  const encoded = encodeURIComponent(email)
  const url = USER_ENDPOINTS.CHECK_EMAIL(encoded)
  const res = await api.get(url, { timeout: 15000 })
  const payload = res.data
  const value = typeof payload === 'boolean' ? payload : (payload?.data ?? payload)
  return value === true
}

// 사용자 정보 조회
export async function getCurrentUser() {
  try {
    const res = await api.get(AUTH_ENDPOINTS.CURRENT_USER)
    if (import.meta.env.DEV) {
      console.log('현재 사용자 정보:', res.data.data)
    }
    currentUserId = res.data.data.id
    if (currentUserId) {
      sessionStorage.setItem('currentUserId', currentUserId)
    }
    return res.data
  } catch (e) {
    console.error('현재 사용자 정보 조회 실패:', e)
    currentUserId = null
    throw e
  }
}

// 현재 저장된 사용자 아이디 동기 반환 함수
export function getCurrentUserId() {
  return currentUserId
}

export async function requestEmailVerification(data: VerificationRequest) {
  try {
    const res = await publicApi.post(USER_ENDPOINTS.EMAIL_VERIFICATION, data)
    return res.data
  } catch (e) {
    if (axios.isAxiosError(e)) {
      console.error('📮 request-verification error', {
        status: e.response?.status,
        url: e.config?.url,
        data: e.response?.data,
      })
    }
    throw e
  }
}
