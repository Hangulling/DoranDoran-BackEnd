import React from 'react'
import type { EnrichedMessage } from '../../pages/ChatPage'
import type { VocabularyExtractedData } from '../../types/sseEvents'
import ChatBubble from './ChatBubble'
import CorrectionBubble from './CorrectionBubble'
import DescriptionBubble from './DescriptionBubble'
import Perfect from '../../assets/chat/correct.svg'

interface ChatMessageItemProps {
  msg: EnrichedMessage
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

const ChatMessageItem: React.FC<ChatMessageItemProps> = React.memo(
  ({ msg, chatroomId, onChatBubbleBookmark, onCorrectionBubbleBookmark }) => {
    return (
      <div className="mb-0">
        <div className="mt-5">
          <ChatBubble
            message={msg.text}
            isSender={msg.isSender}
            avatarUrl={msg.avatarUrl}
            variant={msg.variant ?? 'basic'}
            showIcon={msg.showIcon}
            messageId={msg.id}
            isBookmarked={!!msg.bookmarkId}
            onBookmarkToggle={(messageId, content) =>
              onChatBubbleBookmark(messageId, content, msg.vocabularyData)
            }
          />
        </div>

        {/* 사용자 메시지 교정 */}
        {msg.isSender &&
          (msg.analysisState === 'pending' ? (
            // PENDING
            <CorrectionBubble
              chatRoomId={chatroomId ?? ''}
              messageId={msg.id}
              originalContent={msg.text}
              isSender={true}
              isLoading={true} // 스켈레톤
            />
          ) : msg.isPerfect ? (
            // COMPLETE + PERFECT
            <div className="flex flex-row justify-end text-[#54BDB4] text-[12px] mt-1 font-medium">
              <img src={Perfect} alt="perfect" />
              perfect
            </div>
          ) : msg.correction && msg.correction.correctedSentence ? (
            // COMPLETE + CORRECTION
            <CorrectionBubble
              chatRoomId={chatroomId ?? ''}
              messageId={msg.id}
              originalContent={msg.text}
              correctedContent={msg.correction.correctedSentence}
              descriptionByTab={{
                Kor: msg.correction.feedback.ko,
                Eng: msg.correction.feedback.en,
              }}
              isSender={true}
              isLoading={false}
              isBookmarked={!!msg.bookmarkId}
              onBookmarkToggle={(messageId, content, correctedContent) =>
                onCorrectionBubbleBookmark(
                  messageId,
                  content,
                  correctedContent,
                  msg.correction!.feedback.ko,
                  msg.correction!.feedback.en
                )
              }
            />
          ) : null)}

        {/* 어휘 */}
        {!msg.isSender &&
          msg.vocabularyData &&
          msg.vocabularyData.words &&
          msg.vocabularyData.words.map((vocabWord, idx) => (
            <DescriptionBubble
              key={idx}
              word={vocabWord.word}
              pronunciation={vocabWord.context.roma}
              descriptionByTab={{
                Kor: vocabWord.context.ko,
                Eng: vocabWord.context.en,
              }}
              initialTab="Eng"
            />
          ))}
      </div>
    )
  }
)

export default ChatMessageItem
