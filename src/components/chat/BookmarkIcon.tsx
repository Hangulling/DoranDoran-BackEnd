import React from 'react'
import FilledMark from '../../assets/icon/filledBookmark.svg?react'
import OutlineMark from '../../assets/icon/outlineBookmark.svg?react'

interface BookmarkIconProps {
  isBookmarked: boolean
  onToggle: () => void
}

const BookmarkIcon: React.FC<BookmarkIconProps> = ({ isBookmarked, onToggle }) => {
  return (
    <button onClick={onToggle} className="focus:outline-none" aria-label="북마크">
      {isBookmarked ? <FilledMark /> : <OutlineMark />}
    </button>
  )
}

export default BookmarkIcon
