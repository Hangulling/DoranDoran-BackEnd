import React from 'react'
import type { EnrichedMessage } from '../../pages/ChatPage'
import type { VocabularyExtractedData } from '../../types/sseEvents'
import ChatBubble from './ChatBubble'
import ChatMessageItem from './ChatMessageItem'

const LoadingDot = () => <span className="loading loading-dots loading-[5px] text-gray-200" />

interface ChatMessageListProps {
  messages: EnrichedMessage[]
  isAiResponding: boolean
  sseError: string | null
  inactivityError: boolean
  roomAvatar: string | undefined
  chatroomId: string | undefined
  onChatBubbleBookmark: (
    messageId: string,
    content: string,
    vocabularyData: VocabularyExtractedData | null | undefined
  ) => void
  onCorrectionBubbleBookmark: (
    messageId: string,
    content: string,
    correctedContent: string,
    feedbackKo: string,
    feedbackEn: string
  ) => void
}

const ChatMessageList: React.FC<ChatMessageListProps> = ({
  messages,
  isAiResponding,
  sseError,
  inactivityError,
  roomAvatar,
  chatroomId,
  onChatBubbleBookmark,
  onCorrectionBubbleBookmark,
}) => {
  return (
    <div className="space-y-4">
      {messages.map(msg => (
        <ChatMessageItem
          key={msg.id}
          msg={msg}
          chatroomId={chatroomId}
          onChatBubbleBookmark={onChatBubbleBookmark}
          onCorrectionBubbleBookmark={onCorrectionBubbleBookmark}
        />
      ))}

      {/* 응답 로딩 */}
      {isAiResponding && (
        <div className="mt-5">
          <ChatBubble
            message={<LoadingDot />}
            isSender={false}
            avatarUrl={roomAvatar}
            variant={'basic'}
            showIcon={false}
          />
        </div>
      )}

      {/* 에러 */}
      <div className="mt-5">
        {sseError && (
          <ChatBubble
            avatarUrl={roomAvatar}
            message={'Failed to load AI response'}
            isSender={false}
            variant={'error'}
            showIcon={false}
          />
        )}

        {inactivityError && (
          <ChatBubble
            avatarUrl={roomAvatar}
            message={'Knock knock'}
            isSender={false}
            variant={'error'}
            showIcon={false}
          />
        )}
      </div>

      <div className="h-4" />
    </div>
  )
}

export default ChatMessageList
