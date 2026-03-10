import { useEffect, useRef, useState } from 'react'
import CommonModal from './CommonModal'
import { useNavigate } from 'react-router-dom'
import { useAuthCleanupStore } from '../../stores/useAuthCleanupStore'
import { useUserStore } from '../../stores/useUserStore'
import useClosenessStore from '../../stores/useClosenessStore'
import useRoomIdStore from '../../stores/useRoomIdStore'
import { useCoachStore, useModalStore } from '../../stores/useUiStateStore'

export default function SessionAutoLogout() {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const openedRef = useRef(false)

  // 정리해야 할 작업
  const preLogoutTask = useAuthCleanupStore(state => state.preLogoutTask)
  const setPreLogoutTask = useAuthCleanupStore(state => state.setPreLogoutTask)

  const handleClientSideLogout = () => {
    console.log('자동 로그아웃 실행')

    // LocalStorage 정리
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('currentUserId')
    try {
      localStorage.setItem('session:logout', String(Date.now()))
    } catch {
      console.warn('Failed to set logout flag')
    }

    // Zustand 스토어 리셋
    useUserStore.getState().reset()
    useClosenessStore.getState().reset()
    useRoomIdStore.getState().reset()
    useCoachStore.getState().reset()
    useModalStore.getState().reset()

    setOpen(false)
    openedRef.current = false
    navigate('/login', { replace: true })
  }

  const handleConfirm = async () => {
    if (preLogoutTask) {
      console.log('페이지별 정리 작업을 실행')
      try {
        await preLogoutTask()
      } catch (e) {
        console.error('정리 작업 실행 중 오류:', e)
      } finally {
        setPreLogoutTask(null)
      }
    }

    // API 대신 클라이언트 강제 로그아웃
    handleClientSideLogout()
  }

  useEffect(() => {
    // 이벤트 파라미터 및 관련 로직 제거
    const openModal = () => {
      if (localStorage.getItem('session:manualLogout') === '1') return
      if (openedRef.current) return

      console.log('세션 만료/비활성 감지됨')

      openedRef.current = true
      setOpen(true)
    }

    window.addEventListener('auth:expired', openModal)
    window.addEventListener('auth:inactive', openModal)

    return () => {
      window.removeEventListener('auth:expired', openModal)
      window.removeEventListener('auth:inactive', openModal)
    }
  }, [])

  return (
    <CommonModal
      variant="signup"
      open={open}
      title="You will be logged out shortly"
      description={`You have been automatically logged out due to 1 hour of inactivity.`}
      confirmText="Confirm"
      onConfirm={handleConfirm}
      onCancel={handleConfirm}
    />
  )
}
