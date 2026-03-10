import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import type { EnrichedMessage } from '../../pages/ChatPage'
import { getMessages } from '../../api'
import { getBookmarksByRoomId } from '../../api/archive'

interface UseChatHistoryProps {
  chatroomId: string | undefined
  userId: string
  roomAvatar: string | undefined
  id: string | undefined
  navigate: ReturnType<typeof useNavigate>

  setMessages: React.Dispatch<React.SetStateAction<EnrichedMessage[]>>
  setIsHistoryLoading: React.Dispatch<React.SetStateAction<boolean>>
  setIsNewChat: React.Dispatch<React.SetStateAction<boolean | null>>
  setGreetingState: React.Dispatch<React.SetStateAction<'pending' | 'loading' | 'complete'>>
  setGreetingMsg1: React.Dispatch<React.SetStateAction<string | null>>
  setGreetingMsg2: React.Dispatch<React.SetStateAction<string | null>>
}

export const useChatHistory = ({
  chatroomId,
  userId,
  roomAvatar,
  id,
  navigate,
  setMessages,
  setIsHistoryLoading,
  setIsNewChat,
  setGreetingState,
  setGreetingMsg1,
  setGreetingMsg2,
}: UseChatHistoryProps) => {
  useEffect(() => {
    const fetchHistory = async () => {
      if (!chatroomId || !userId) {
        setIsHistoryLoading(false)
        return
      }
      try {
        const [messagesResponse, bookmarksResponse] = await Promise.all([
          getMessages(chatroomId, { userId }),
          getBookmarksByRoomId(chatroomId),
        ])

        const historyMessages = messagesResponse.content

        // 북마크 데이터 조회
        const bookmarkMap = new Map<string, string>()
        if (Array.isArray(bookmarksResponse)) {
          bookmarksResponse.forEach(bookmark => {
            if (bookmark.messageId && bookmark.id) {
              bookmarkMap.set(bookmark.messageId, bookmark.id)
            }
          })
        }

        const botGreeting =
          historyMessages.length > 0 &&
          historyMessages[0].senderType === 'bot' &&
          historyMessages[0].metadata === null
            ? historyMessages[0]
            : null

        const guideGreeting = historyMessages.find(msg => msg.senderType === 'system') ?? null

        // 실제 대화내역 필터링
        const conversationMessages = historyMessages.filter(
          msg => msg.id !== botGreeting?.id && msg.id !== guideGreeting?.id
        )

        const enrichedHistory: EnrichedMessage[] = []
        for (let i = 0; i < conversationMessages.length; i++) {
          const apiMsg = conversationMessages[i]

          const baseMessage: EnrichedMessage = {
            id: apiMsg.id,
            text: apiMsg.content,
            isSender: apiMsg.senderType === 'user',
            avatarUrl: apiMsg.senderType !== 'user' ? roomAvatar : undefined,
            variant: apiMsg.senderType === 'user' ? 'sender' : 'basic',
            showIcon: apiMsg.senderType !== 'user',
            bookmarkId: bookmarkMap.get(apiMsg.id) || null,
            analysisState: 'complete', // 히스토리는 항상 'complete'
          }

          // 히스토리 파싱 로직
          if (apiMsg.senderType === 'user') {
            const nextMsg = conversationMessages[i + 1]
            let foundAnalysis = false

            if (
              nextMsg &&
              nextMsg.senderType === 'bot' &&
              nextMsg.metadata?.userMessageAnalysis?.userMessageId === apiMsg.id
            ) {
              const intimacy = nextMsg.metadata.userMessageAnalysis.intimacy
              if (
                intimacy &&
                intimacy.correctedSentence &&
                (intimacy.corrections || intimacy.feedback?.ko)
              ) {
                baseMessage.correction = {
                  messageId: nextMsg.metadata.userMessageAnalysis.userMessageId,
                  corrections: intimacy.corrections,
                  feedback: intimacy.feedback,
                  correctedSentence: intimacy.correctedSentence,
                  detectedLevel: intimacy.detectedLevel,
                }
                baseMessage.isPerfect = false
                foundAnalysis = true
              } else if (intimacy) {
                baseMessage.correction = null
                baseMessage.isPerfect = true
                foundAnalysis = true
              }
            }
            if (!foundAnalysis) {
              baseMessage.correction = null
              baseMessage.isPerfect = false
            }
          } else if (apiMsg.senderType === 'bot') {
            if (apiMsg.metadata?.botResponseAnalysis?.vocabulary) {
              baseMessage.vocabularyData = apiMsg.metadata.botResponseAnalysis.vocabulary
            }
          }
          enrichedHistory.push(baseMessage)
        }

        // 'enrichedHistory' (필터링된) 기준으로 분기 처리
        if (enrichedHistory.length === 0 && !botGreeting) {
          // Case 1: 정말 새로운 채팅 (히스토리 0개)
          setIsNewChat(true)
          setGreetingState('loading') // SSE 로딩 시작
        } else {
          // Case 2: 새로고침 (대화내역이 있거나, 그리팅 메시지만 있음)
          setIsNewChat(false)
          if (botGreeting) {
            setGreetingMsg1(botGreeting.content)
            setGreetingMsg2(guideGreeting?.content ?? null)
            setGreetingState('complete') // 즉시 '완료'
          } else {
            setGreetingState('complete')
          }
        }

        // 불러온 기록(그리팅 제외)으로 messages 상태를 초기화
        setMessages(enrichedHistory)
      } catch (error) {
        navigate('/error', { state: { from: `/chat/${id}` } })
        console.error('Failed to fetch chat history:', error)
      } finally {
        setIsHistoryLoading(false)
      }
    }

    fetchHistory()
  }, [
    chatroomId,
    userId,
    roomAvatar,
    id,
    navigate,
    setMessages,
    setIsHistoryLoading,
    setIsNewChat,
    setGreetingState,
    setGreetingMsg1,
    setGreetingMsg2,
  ])
}
