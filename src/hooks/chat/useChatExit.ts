import { useState, useRef, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useModalStore } from '../../stores/useUiStateStore'
import { useAuthCleanupStore } from '../../stores/useAuthCleanupStore'
import { leaveChatroom, updateUser } from '../../api'
import { useUserMsgStore } from '../../stores/useUserMsgStore'
import ReactGA from 'react-ga4'
import { GA_ENABLED, IS_PROD } from '../../constants/env'

interface UseChatExitProps {
  chatroomId: string | undefined
  userId: string
  routeId: string | undefined
}

export const useChatExit = ({ chatroomId, userId, routeId }: UseChatExitProps) => {
  const navigate = useNavigate()
  const [isModalOpen, setIsModalOpen] = useState(false)
  const hasLeftRef = useRef(false) // 중복 나가기 방지

  const noShowAgain = useModalStore(state => state.noShowAgain)
  const setPreLogoutTask = useAuthCleanupStore(state => state.setPreLogoutTask)

  // 공통 채팅방 나가기
  const performLeaveChatroom = useCallback(async () => {
    if (!chatroomId || !userId) return

    try {
      await leaveChatroom(chatroomId, userId)
      console.log('채팅방 나가기 성공')

      sessionStorage.removeItem(`initChat_${routeId}`)
      sessionStorage.removeItem(`viewed_chatroom_${chatroomId}`)
      useUserMsgStore.getState().clearUserMsgs()
    } catch (error) {
      console.error('채팅방 나가기 실패:', error)
      throw error
    }
  }, [chatroomId, userId, routeId])

  const handleConfirmExit = useCallback(async () => {
    if (!chatroomId) {
      console.error('채팅방 ID가 없어 나갈 수 없습니다.')
      setIsModalOpen(false)
      return
    }

    if (hasLeftRef.current) return
    hasLeftRef.current = true

    // leave_chatroom
    if (IS_PROD && GA_ENABLED && userId) {
      const leaveTimestamp = Math.floor(Date.now() / 1000)
      ReactGA.event('leave_chatroom', {
        chatroom_id: chatroomId,
        leave_timestamp: leaveTimestamp,
      })
    }

    // 다시 보지 않기 설정
    if (noShowAgain) {
      try {
        await updateUser(userId, { exitModalDoNotShowAgain: true })
      } catch (e) {
        console.error('사용자 설정 업데이트 실패:', e)
      }
    }

    try {
      await performLeaveChatroom()
      setIsModalOpen(false)
      navigate('/', { replace: true })
    } catch (error) {
      console.error('채팅방 나가기 중 오류 발생:', error)
      hasLeftRef.current = false
      setIsModalOpen(false)
    }
  }, [chatroomId, userId, noShowAgain, navigate, performLeaveChatroom])

  const handleCancelExit = useCallback(() => {
    setIsModalOpen(false)
  }, [])

  // 자동 로그아웃 시 정리
  useEffect(() => {
    if (chatroomId && userId) {
      const cleanupTask = async () => {
        if (hasLeftRef.current) return
        hasLeftRef.current = true
        console.log('자동 로그아웃으로 인한 채팅방 나가기 실행')
        await performLeaveChatroom()
      }
      setPreLogoutTask(cleanupTask)
    } else {
      setPreLogoutTask(null)
    }

    return () => {
      setPreLogoutTask(null)
    }
  }, [chatroomId, userId, setPreLogoutTask, performLeaveChatroom, hasLeftRef])

  // 브라우저 뒤로가기(popstate) / 탭 닫기(beforeunload)
  useEffect(() => {
    window.history.pushState(null, '', window.location.href)

    // OS 뒤로가기
    const handlePopState = async () => {
      if (hasLeftRef.current) return

      if (noShowAgain) {
        // 다시 보지 않기
        hasLeftRef.current = true
        try {
          await performLeaveChatroom()
        } catch (e) {
          console.error('Failed to leave chatroom (noShowAgain):', e)
        }
        navigate('/')
      } else {
        setIsModalOpen(true)
        window.history.pushState(null, '', window.location.href)
      }
    }
    window.addEventListener('popstate', handlePopState)

    // 탭 닫기 / 새로고침
    const handleUnload = () => {
      if (hasLeftRef.current) return

      // 새로고침인지 확인
      const navigationEntries = performance.getEntriesByType('navigation')
      if (navigationEntries.length > 0) {
        const navEntry = navigationEntries[0] as PerformanceNavigationTiming
        if (navEntry.type === 'reload') {
          console.log('[ChatPage] Refresh detected. NOT leaving chatroom.')
          return
        }
      }

      // 탭 닫기
      if (chatroomId && userId) {
        const accessToken = sessionStorage.getItem('accessToken')
        if (!accessToken) return

        const API_BASE_URL = import.meta.env.VITE_API_BASE_URL
        const url = `${API_BASE_URL}/api/chat/chatrooms/${chatroomId}/leave?userId=${userId}`

        try {
          fetch(url, {
            method: 'POST',
            headers: { Authorization: `Bearer ${accessToken}` },
            keepalive: true, // 페이지가 닫혀도 요청
          })
        } catch (e) {
          console.error('Failed to send keepalive fetch:', e)
        }
      }
    }
    window.addEventListener('beforeunload', handleUnload)

    return () => {
      window.removeEventListener('popstate', handlePopState)
      window.removeEventListener('beforeunload', handleUnload)
    }
  }, [chatroomId, userId, navigate, noShowAgain, routeId, performLeaveChatroom, hasLeftRef])

  return {
    isModalOpen,
    handleConfirmExit,
    handleCancelExit,
  }
}
