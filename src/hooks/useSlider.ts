import { useState } from 'react'
import type { PanInfo } from 'framer-motion'

export const useSlider = (totalLength: number) => {
  const [[page, direction], setPage] = useState([0, 0])

  const paginate = (newDirection: number) => {
    const newPage = page + newDirection
    if (newPage >= 0 && newPage < totalLength) {
      setPage([newPage, newDirection])
    }
  }

  // 화면 터치 핸들러
  const handleScreenClick = (e: React.MouseEvent) => {
    const screenWidth = window.innerWidth
    const clickX = e.clientX
    if (clickX > screenWidth * 0.55) paginate(1)
    else if (clickX < screenWidth * 0.45) paginate(-1)
  }

  // 드래그 종료 핸들러
  const handleDragEnd = (_: unknown, { offset, velocity }: PanInfo) => {
    const swipeConfidenceThreshold = 10000
    const swipePower = Math.abs(offset.x) * velocity.x

    if (swipePower < -swipeConfidenceThreshold) paginate(1)
    else if (swipePower > swipeConfidenceThreshold) paginate(-1)
  }

  return {
    page,
    direction,
    paginate,
    handleScreenClick,
    handleDragEnd,
  }
}
