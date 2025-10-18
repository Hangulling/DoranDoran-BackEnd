import { useEffect } from 'react'

// 브라우저 뒤로 가기와 같은 동작
export const useGoBack = (onBack?: () => void) => {
  useEffect(() => {
    window.history.pushState(null, '', window.location.href)

    const handlePopState = () => {
      window.history.pushState(null, '', window.location.href)
      onBack?.()
    }

    window.addEventListener('popstate', handlePopState)

    return () => {
      window.removeEventListener('popstate', handlePopState)
    }
  }, [onBack])
}
