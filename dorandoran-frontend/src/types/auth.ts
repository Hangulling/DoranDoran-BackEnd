import type { User } from './user'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  success: boolean
  message: string
  data: {
    accessToken: string
    refreshToken: string
    tokenType: 'Bearer'
    expiresIn: number
    user: User
  }
}

export interface SignupRequest {
  email: string
  firstName: string
  lastName: string
  name: string
  password: string
  picture?: string
  info?: string
}

export interface SignupResponse {
  id: string
  email: string
  firstName: string
  lastName: string
  name: string
  status: string
  role: string
  createdAt: string
  updatedAt: string
}

export interface LogoutResponse {
  success: boolean
  message: string
  data: null
  errorCode: string | null
}
