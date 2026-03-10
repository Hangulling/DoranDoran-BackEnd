import { useState, useEffect } from 'react'

export function useSidebarAnimation(isOpen: boolean, delay = 300) {
  const [visible, setVisible] = useState(false)
  const [isActive, setIsActive] = useState(false)

  useEffect(() => {
    if (isOpen) {
      setVisible(true)
      const timer = setTimeout(() => setIsActive(true), 150)
      return () => clearTimeout(timer)
    } else {
      setIsActive(false)
      const timer = setTimeout(() => setVisible(false), delay)
      return () => clearTimeout(timer)
    }
  }, [isOpen, delay])

  return { visible, isActive }
}
