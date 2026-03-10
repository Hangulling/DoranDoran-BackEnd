import { useCallback } from 'react'
import type { EnrichedMessage } from '../../pages/ChatPage'
import type { VocabularyExtractedData } from '../../types/sseEvents'
import { createBookmark, deleteBookmark } from '../../api/archive'
import showToast from '../../components/common/CommonToast'

interface UseBookmarkManagerProps {
  chatroomId: string | undefined
  chatbotId: string
  closenessText: string
  messages: EnrichedMessage[]
  setMessages: React.Dispatch<React.SetStateAction<EnrichedMessage[]>>
}

export const useBookmarkManager = ({
  chatroomId,
  chatbotId,
  closenessText,
  messages,
  setMessages,
}: UseBookmarkManagerProps) => {
  // 북마크 생성
  const handleAddBookmark = useCallback(
    async (
      messageId: string,
      options: {
        content: string
        correctedContent?: string
        feedbackKo?: string
        feedbackEn?: string
        vocabularyData?: VocabularyExtractedData | null
      }
    ): Promise<string | null> => {
      if (!chatroomId) {
        console.error('Bookmark: chatroomId가 없습니다.')
        return null
      }

      try {
        const aiResponse: {
          intimacyLevel: string
          description?: string
          translation?: { english: string }
          vocabulary?: {
            word: string
            pronunciation: string
            explanation: string
            korExplanation: string
          }[]
        } = {
          intimacyLevel: closenessText,
        }

        if (options.feedbackKo) {
          aiResponse.description = options.feedbackKo
        }
        if (options.feedbackEn) {
          aiResponse.translation = { english: options.feedbackEn }
        }
        if (options.vocabularyData && options.vocabularyData.words) {
          aiResponse.vocabulary = options.vocabularyData.words.map(vocabWord => ({
            word: vocabWord.word,
            pronunciation: vocabWord.context.roma,
            explanation: vocabWord.context.en,
            korExplanation: vocabWord.context.ko,
          }))
        }

        const requestBody = {
          messageId,
          chatroomId: chatroomId,
          chatbotId: chatbotId,
          content: options.content,
          correctedContent: options.correctedContent,
          aiResponse: aiResponse,
        }

        const response = await createBookmark(requestBody)
        console.log('북마크 추가 성공', response)
        return response.id
      } catch (error) {
        showToast({ message: 'Failed to save', iconType: 'error' })
        console.error('북마크 추가 실패', error)
        return null
      }
    },
    [chatroomId, chatbotId, closenessText]
  )

  // 일반 버블 북마크 토글
  const handleChatBubbleBookmark = useCallback(
    async (
      messageId: string,
      content: string,
      vocabularyData: VocabularyExtractedData | null | undefined
    ) => {
      const message = messages.find(m => m.id === messageId)
      if (!message) return

      if (message.bookmarkId) {
        // 북마크 삭제
        try {
          await deleteBookmark(message.bookmarkId)
          setMessages(prev => prev.map(m => (m.id === messageId ? { ...m, bookmarkId: null } : m)))
          console.log('북마크 삭제 성공')
        } catch (error) {
          console.error('북마크 삭제 실패', error)
        }
      } else {
        // 북마크 추가
        const newBookmarkId = await handleAddBookmark(messageId, { content, vocabularyData })
        if (newBookmarkId) {
          setMessages(prev =>
            prev.map(m => (m.id === messageId ? { ...m, bookmarkId: newBookmarkId } : m))
          )
        }
      }
    },
    [messages, setMessages, handleAddBookmark]
  )

  // 교정 버블 북마크 토글
  const handleCorrectionBubbleBookmark = useCallback(
    async (
      messageId: string,
      content: string,
      correctedContent: string,
      feedbackKo: string,
      feedbackEn: string
    ) => {
      const message = messages.find(m => m.id === messageId)
      if (!message) return

      if (message.bookmarkId) {
        // 북마크 삭제
        try {
          await deleteBookmark(message.bookmarkId)
          setMessages(prev => prev.map(m => (m.id === messageId ? { ...m, bookmarkId: null } : m)))
        } catch (error) {
          console.error('북마크 삭제 실패', error)
        }
      } else {
        // 북마크 추가
        const newBookmarkId = await handleAddBookmark(messageId, {
          content,
          correctedContent,
          feedbackKo,
          feedbackEn,
        })
        if (newBookmarkId) {
          setMessages(prev =>
            prev.map(m => (m.id === messageId ? { ...m, bookmarkId: newBookmarkId } : m))
          )
        }
      }
    },
    [messages, setMessages, handleAddBookmark]
  )

  return {
    handleChatBubbleBookmark,
    handleCorrectionBubbleBookmark,
  }
}
