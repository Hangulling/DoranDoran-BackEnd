/**
 * Admin 전용 PrivateRoute
 * 기존 PrivateRoute와 분리하여 관리
 * role이 ROLE_ADMIN인 경우에만 접근 허용
 */
import type { JSX } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

interface AdminPrivateRouteProps {
  children: JSX.Element
}

export default function AdminPrivateRoute({ children }: AdminPrivateRouteProps) {
  const location = useLocation()
  const token = sessionStorage.getItem('accessToken')
  const adminUserStr = sessionStorage.getItem('adminUser')

  // 토큰이 없으면 로그인 페이지로 리다이렉트
  if (!token) {
    console.log('No token found, redirecting to login.')
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  // Admin 사용자 정보가 없거나 role이 ROLE_ADMIN이 아니면 로그인 페이지로 리다이렉트
  if (!adminUserStr) {
    console.log('No admin user found, redirecting to login.')
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  try {
    const adminUser = JSON.parse(adminUserStr)
    if (adminUser.role !== 'ROLE_ADMIN') {
      console.log('User is not an admin, redirecting to login.')
      return <Navigate to="/login" state={{ from: location }} replace />
    }
  } catch (e) {
    console.error('Failed to parse admin user:', e)
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return children
}
