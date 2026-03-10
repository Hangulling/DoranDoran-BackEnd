import { useEffect, useRef, useState } from 'react'
import { useCoachStore } from '../../stores/useUiStateStore'
import { getUserById, updateUser } from '../../api'

export const useCoachMark = (userId: string, isInitChatReady: boolean) => {
  const coachMarkSeen = useCoachStore(s => s.coachMarkSeen)
  const setCoachMarkSeen = useCoachStore(s => s.setCoachMarkSeen)

  const [showCoachMark, setShowCoachMark] = useState(false)
  const coachTimerRef = useRef<number | null>(null)

  useEffect(() => {
    if (coachMarkSeen || !isInitChatReady || coachTimerRef.current || !userId) {
      return
    }

    const checkCoachMark = async () => {
      try {
        const user = await getUserById(userId)
        if (user?.coachCheck === false) {
          coachTimerRef.current = window.setTimeout(() => {
            requestAnimationFrame(() => {
              setShowCoachMark(true)
              coachTimerRef.current = null
            })
          }, 600) // 0.6초 딜레이
        }
      } catch (e) {
        console.error('유저 정보 조회 실패', e)
      }
    }

    checkCoachMark()
  }, [userId, coachMarkSeen, isInitChatReady])

  const handleCloseCoachMark = async () => {
    setShowCoachMark(false)
    try {
      await updateUser(userId, { coachCheck: true })
      setCoachMarkSeen(true)
    } catch (e) {
      console.error('coachCheck 업데이트 실패', e)
    }
  }

  // 컴포넌트 언마운트 시 타이머 클린업
  useEffect(() => {
    return () => {
      if (coachTimerRef.current) {
        window.clearTimeout(coachTimerRef.current)
      }
    }
  }, [])

  return {
    showCoachMark,
    handleCloseCoachMark,
  }
}
