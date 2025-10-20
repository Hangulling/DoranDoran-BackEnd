// 사용자 생성 시 요청
export interface CreatePayload {
  email: string // 이메일 형식
  firstName: string // 1-50자
  lastName: string // 1-50자
  name: string // 미제공 시 `firstName + lastName` 사용
  password: string // 8-100자
  picture?: string
  info?: string // 최대 100자
}

//사용자 정보 응답
export interface User {
  id: string // UUID
  email: string
  firstName: string
  lastName: string
  name: string
  passwordHash: string
  picture: string
  info: string
  lastConnTime: string // ISO 8601 날짜 문자열
  status: UserStatus
  role: UserRole
  coachCheck: boolean
  exitModalDoNotShowAgain: boolean
  createdAt: string
  updatedAt: string
}

// 사용자 정보 업데이트 요청
export interface UpdatePayload {
  firstName?: string
  lastName?: string
  name?: string
  picture?: string
  info?: string
  email?: string
  status?: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
  coachCheck?: boolean
  exitModalDoNotShowAgain?: boolean
}

// 사용자 상태
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'

// 사용자 역할?
export type UserRole = 'ROLE_USER' | 'ROLE_ADMIN'
