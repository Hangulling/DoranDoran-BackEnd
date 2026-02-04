import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useUserStore } from '../stores/useUserStore'
import { getCurrentUser } from '../api'
import ReactGA from 'react-ga4'
import { GA_ENABLED, IS_PROD } from '../constants/env'

export const useFetchUser = () => {
  const navigate = useNavigate()
  const [userName, setUserName] = useState<string>('')
  const [userId, setUserId] = useState<string>('')
  const setStoreName = useUserStore(state => state.setName)
  const setStoreId = useUserStore(state => state.setId)

  useEffect(() => {
    async function fetchUser() {
      try {
        const response = await getCurrentUser()
        const profile = response.data
        setUserName(profile.name)
        setStoreName(profile.name)
        setUserId(profile.id)
        setStoreId(profile.id)

        // 로그인 후 메인페이지 진입 시, User-ID 재설정
        if (IS_PROD && GA_ENABLED) {
          ReactGA.set({ user_id: profile.id })
        }
      } catch (err) {
        console.error('사용자 정보 로드 실패:', err)
        navigate('/error', { state: { from: '/main' } })
      }
    }
    fetchUser()
  }, [setStoreName, setStoreId, navigate])

  return { userName, userId }
}
