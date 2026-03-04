import React from 'react'
import TTSIcon from './VolumeIcon'
import BookmarkIcon from './BookmarkIcon'
import useTTS from '../../hooks/useTTS'

interface ChatBubbleProps {
  message: React.ReactNode
  isSender: boolean
  avatarUrl?: string
  variant?: 'basic' | 'second' | 'sender' | 'error'
  showIcon?: boolean
  messageId?: string // 북마크
  isBookmarked?: boolean
  onBookmarkToggle?: (messageId: string, content: string) => void // 북마크
}

const bubbleVariants = {
  basic: 'bg-white border border-gray-100 rounded-lg rounded-tl-none', // 기본 답장
  second: 'bg-white border border-gray-100 rounded-lg', // 두번째 답장
  sender: 'bg-green-400 text-white rounded-lg rounded-tr-none', // 사용자 채팅
  error: 'bg-white border border-orange-100 rounded-lg rounded-tl-none text-orange-200', // 에러 채팅
}

const ChatBubble: React.FC<ChatBubbleProps> = ({
  message,
  isSender,
  avatarUrl,
  variant = 'basic',
  showIcon = false,
  messageId,
  isBookmarked,
  onBookmarkToggle,
}) => {
  const baseBubbleClass = 'py-[6px] px-2 text-[14px] max-w-[265px] rounded-lg'
  const bubbleClass = `${baseBubbleClass} ${isSender ? bubbleVariants.sender : bubbleVariants[variant]}`
  const marginClass =
    !isSender && (avatarUrl || variant === 'second' || variant === 'error') ? 'ml-10' : ''

  const ttsText = typeof message === 'string' ? message : ''
  const { onPlay: playTTS, playing: isPlaying } = useTTS(ttsText)

  const toggleBookmark = () => {
    if (onBookmarkToggle && messageId && typeof message === 'string') {
      onBookmarkToggle(messageId, message)
    }
  }

  return (
    <div
      className={
        isSender
          ? 'chat chat-end gap-x-[0] pt-0 pb-0'
          : 'chat chat-start gap-x-[8px] relative pt-0 pb-0'
      }
    >
      {!isSender && avatarUrl && (variant === 'basic' || 'error') && (
        <div className="chat-image avatar absolute top-0 left-0 w-8 h-8">
          <div className="w-8 h-8 rounded-full overflow-hidden">
            <img alt="프로필 사진" src={avatarUrl} />
          </div>
        </div>
      )}

      <div className={`${bubbleClass} ${marginClass} flex flex-col max-w-[265px]`}>
        <span>{message}</span>

        {!isSender && showIcon && (
          <>
            <div className="h-[1px] bg-gray-80 w-full my-1" />
            <div className="flex flex-row justify-between">
              <TTSIcon playing={isPlaying} onPlay={playTTS} />
              <BookmarkIcon isBookmarked={isBookmarked ?? false} onToggle={toggleBookmark} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}

export default ChatBubble
