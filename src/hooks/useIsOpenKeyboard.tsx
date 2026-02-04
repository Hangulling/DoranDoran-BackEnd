import { useEffect, useRef, useState } from 'react'
import { useIsMobile } from './useIsMobile' // 모바일 환경에서만 사용하도록

export interface KeyboardStatus {
  isOpen: boolean
  keyboardHeight: number
  viewportHeight: number | null
}

export const useIsOpenKeyboard = (threshold = 100) => {
  const isMobile = useIsMobile()
  const initialHeight = useRef<number | null>(null)
  const [status, setStatus] = useState<KeyboardStatus>({
    isOpen: false,
    keyboardHeight: 0,
    viewportHeight: null,
  })

  useEffect(() => {
    if (!isMobile || !visualViewport) return

    initialHeight.current = visualViewport.height

    const handleResize = () => {
      if (visualViewport && initialHeight.current) {
        const currentHeight = visualViewport.height
        // 임계값 이상 차이날 때만 키보드가 열린 것으로 판단
        const isKeyboardVisible = initialHeight.current - currentHeight > threshold
        const keyboardHeight = isKeyboardVisible ? initialHeight.current - currentHeight : 0

        setStatus({
          isOpen: isKeyboardVisible,
          keyboardHeight: keyboardHeight,
          viewportHeight: currentHeight,
        })
      }
    }

    // 초기 상태 설정
    handleResize()

    visualViewport.addEventListener('resize', handleResize)
    window.addEventListener('orientationchange', () => {
      // 방향 전환 시 초기 높이 재설정
      setTimeout(() => {
        if (visualViewport) initialHeight.current = visualViewport.height
        handleResize()
      }, 300)
    })

    return () => {
      visualViewport?.removeEventListener('resize', handleResize)
      window.removeEventListener('orientationchange', handleResize)
    }
  }, [isMobile, threshold])

  return status
}
