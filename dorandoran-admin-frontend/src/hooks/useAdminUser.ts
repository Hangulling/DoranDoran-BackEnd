import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdminUser, type AdminUser } from '../api/admin'

export function useAdminUser() {
  const navigate = useNavigate()
  const [user, setUser] = useState<AdminUser | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function fetchUser() {
      try {
        setLoading(true)
        const userData = await getAdminUser()
        setUser(userData)
        setError(null)
      } catch (err) {
        console.error('Admin 사용자 정보 로드 실패:', err)
        setError(err instanceof Error ? err.message : 'Unknown error')
        // 인증 실패 시 admin 로그인 페이지로 리다이렉트
        navigate('/login')
      } finally {
        setLoading(false)
      }
    }

    fetchUser()
  }, [navigate])

  return { user, loading, error }
}
