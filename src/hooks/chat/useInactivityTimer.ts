import { useState, useRef, useEffect, useCallback } from 'react'

export const useInactivityTimer = (duration: number) => {
  const [inactivityError, setInactivityError] = useState(false)
  const inactivityTimerRef = useRef<NodeJS.Timeout | null>(null)

  // 타이머 리셋 및 재시작
  const resetInactivityTimer = useCallback(() => {
    // 기존 타이머가 있으면 제거
    if (inactivityTimerRef.current) {
      clearTimeout(inactivityTimerRef.current)
    }

    setInactivityError(false)

    // 새 타이머 설정
    inactivityTimerRef.current = setTimeout(() => {
      console.warn(`${duration / 1000}초간 활동이 없어 비활성 에러 표시`)
      setInactivityError(true)
    }, duration)
  }, [duration])

  // 타이머 중지
  const stopInactivityTimer = useCallback(() => {
    if (inactivityTimerRef.current) {
      clearTimeout(inactivityTimerRef.current)
    }
    setInactivityError(false)
  }, [])

  // 타이머 시작
  useEffect(() => {
    resetInactivityTimer()
    // 언마운트 시 타이머 완전 제거
    return () => {
      if (inactivityTimerRef.current) {
        clearTimeout(inactivityTimerRef.current)
      }
    }
  }, [resetInactivityTimer])

  return {
    inactivityError,
    resetInactivityTimer,
    stopInactivityTimer,
  }
}
