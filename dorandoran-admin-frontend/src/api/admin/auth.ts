/**
 * Admin 전용 인증 API
 * 기존 src/api/auth.ts와 분리하여 관리
 */
import type { LoginRequest, LoginResponse, OAuthLoginRequest, OAuthLoginResponse } from '../../types/auth'
import { publicApi } from '../api'
import { AUTH_ENDPOINTS } from '../endpoints'

/**
 * Admin 전용 로그인 (이메일/비밀번호)
 * 로그인 성공 시 role이 ROLE_ADMIN인지 확인 필요
 */
export async function adminLogin(data: LoginRequest): Promise<LoginResponse> {
  const res = await publicApi.post<LoginResponse>(AUTH_ENDPOINTS.LOGIN, data)
  const { data: resData } = res.data
  const { accessToken, refreshToken, user } = resData

  // Admin role 체크
  if (user.role !== 'ROLE_ADMIN') {
    throw new Error('관리자 권한이 필요합니다.')
  }

  // 토큰 저장
  if (accessToken) {
    sessionStorage.setItem('accessToken', accessToken)
  }

  if (refreshToken) {
    sessionStorage.setItem('refreshToken', refreshToken)
  }

  // 사용자 정보 저장 (role 체크용)
  sessionStorage.setItem('adminUser', JSON.stringify(user))

  return res.data
}

/**
 * Admin 전용 OAuth 로그인
 * 로그인 성공 시 role이 ROLE_ADMIN인지 확인 필요
 */
export async function adminOAuthLogin(data: OAuthLoginRequest): Promise<OAuthLoginResponse> {
  const res = await publicApi.post<OAuthLoginResponse>(AUTH_ENDPOINTS.OAUTH_LOGIN, data)
  const { data: resData } = res.data
  const { accessToken, refreshToken, user } = resData

  // Admin role 체크
  if (user.role !== 'ROLE_ADMIN') {
    throw new Error('관리자 권한이 필요합니다.')
  }

  // 토큰 저장
  if (accessToken) {
    sessionStorage.setItem('accessToken', accessToken)
  }

  if (refreshToken) {
    sessionStorage.setItem('refreshToken', refreshToken)
  }

  // 사용자 정보 저장 (role 체크용)
  sessionStorage.setItem('adminUser', JSON.stringify(user))

  return res.data
}

/**
 * Admin 로그아웃
 */
export async function adminLogout() {
  sessionStorage.removeItem('accessToken')
  sessionStorage.removeItem('refreshToken')
  sessionStorage.removeItem('adminUser')
  sessionStorage.setItem('session:manualLogout', '1')
}
