import { useCallback, useRef, useState } from 'react'
import { useChatStream } from './useChatStream'
import type { EnrichedMessage } from '../../pages/ChatPage'
import type {
  EventDataMap,
  GreetingBotMessageData,
  GreetingGuideMessageData,
  IntimacyAnalysisData,
  VocabularyExtractedData,
} from '../../types/sseEvents'
import { sendMessage } from '../../api'
import { useUserMsgStore } from '../../stores/useUserMsgStore'
import ReactGA from 'react-ga4'
import { GA_ENABLED, IS_PROD } from '../../constants/env'

interface UseChatInteractionProps {
  chatroomId: string | undefined
  userId: string
  accessToken: string
  roomAvatar: string | undefined
  isNewChat: boolean | null

  setMessages: React.Dispatch<React.SetStateAction<EnrichedMessage[]>>
  resetInactivityTimer: () => void
  stopInactivityTimer: () => void

  setGreetingMsg1: React.Dispatch<React.SetStateAction<string | null>>
  setGreetingMsg2: React.Dispatch<React.SetStateAction<string | null>>
  setGreetingState: React.Dispatch<React.SetStateAction<'pending' | 'loading' | 'complete'>>
}

export const useChatInteraction = ({
  chatroomId,
  userId,
  accessToken,
  roomAvatar,
  isNewChat,
  setMessages,
  resetInactivityTimer,
  stopInactivityTimer,
  setGreetingMsg1,
  setGreetingMsg2,
  setGreetingState,
}: UseChatInteractionProps) => {
  const [isAiResponding, setIsAiResponding] = useState(false)
  const [sseError, setSseError] = useState<string | null>(null)

  const pendingVocabularyRef = useRef<VocabularyExtractedData | null>(null)
  const lastUserMsgIdRef = useRef<string | null>(null)
  const lastAiMsgIdRef = useRef<string | null>(null)

  // 메시지 전송
  const handleSendMessage = async (text: string) => {
    resetInactivityTimer() // 타이머 리셋

    if (!chatroomId) {
      console.error('채팅방 ID가 없습니다.')
      return
    }

    try {
      const response = await sendMessage(chatroomId, {
        senderType: 'user',
        content: text,
        contentType: 'text',
      })

      if (IS_PROD && GA_ENABLED) {
        ReactGA.event('send_user_message', {
          chatroom_id: chatroomId,
          user_message: text,
        })
      }

      useUserMsgStore.getState().addUserMsg({
        id: response.id,
        content: response.content,
      })

      lastUserMsgIdRef.current = response.id // 마지막 사용자 메시지 ID 저장
      const newMessage: EnrichedMessage = {
        id: response.id,
        text: response.content,
        isSender: true,
        variant: 'sender',
        analysisState: 'pending',
      }
      setMessages(prevMessages => [...prevMessages, newMessage])
      setIsAiResponding(true)
    } catch (error) {
      console.error('메시지 전송 실패:', error)
      setIsAiResponding(false)
    }
  }

  // SSE 이벤트 처리
  const handleSseEvent = useCallback(
    (eventType: string, data: unknown) => {
      resetInactivityTimer() // 타이머 리셋
      setSseError(null) // 이벤트 수신되면 에러 초기화

      switch (eventType) {
        case 'greeting_bot_message': {
          if (!isNewChat) break
          setGreetingMsg1((data as GreetingBotMessageData).content)
          setGreetingState('loading')
          break
        }
        case 'greeting_guide_message': {
          if (!isNewChat) break
          setGreetingMsg2((data as GreetingGuideMessageData).content)
          break
        }
        case 'conversation_complete': {
          const conversationData = data as EventDataMap['conversation_complete']
          const vocabData = pendingVocabularyRef.current

          setIsAiResponding(false) // 로딩 종료

          if (IS_PROD && GA_ENABLED && chatroomId) {
            ReactGA.event('send_ai_reply', {
              chatroom_id: chatroomId,
              intimacy_message: conversationData.content,
            })
          }

          lastAiMsgIdRef.current = conversationData.messageId

          const newAiMessage: EnrichedMessage = {
            id: conversationData.messageId,
            text: conversationData.content,
            isSender: false,
            avatarUrl: roomAvatar ?? '',
            variant: 'basic',
            showIcon: true,
            vocabularyData: vocabData,
          }
          setMessages(prev => [...prev, newAiMessage])
          pendingVocabularyRef.current = null // 대기 데이터 비우기
          break
        }
        case 'intimacy_analysis': {
          const intimacyData = data as IntimacyAnalysisData

          if (IS_PROD && GA_ENABLED && chatroomId) {
            ReactGA.event('send_ai_intimacy', {
              chatroom_id: chatroomId,
              ai_message: intimacyData.corrections ? intimacyData.correctedSentence : '',
            })
          }

          const targetMsgId = lastUserMsgIdRef.current
          if (!targetMsgId) break

          setMessages(prev =>
            prev.map(msg => {
              if (msg.id === targetMsgId) {
                if (intimacyData && intimacyData.correctedSentence && intimacyData.corrections) {
                  return {
                    ...msg,
                    correction: intimacyData,
                    isPerfect: false,
                    analysisState: 'complete',
                  }
                } else {
                  return { ...msg, correction: null, isPerfect: true, analysisState: 'complete' }
                }
              }
              return msg
            })
          )
          lastUserMsgIdRef.current = null
          break
        }
        case 'vocabulary_extracted': {
          const vocabData = data as VocabularyExtractedData
          pendingVocabularyRef.current = vocabData
          break
        }
        default:
          console.warn(`[SSE] Unhandled event type: ${eventType}`, data)
          break
      }
    },
    [
      roomAvatar,
      isNewChat,
      resetInactivityTimer,
      chatroomId,
      setMessages,
      setGreetingMsg1,
      setGreetingState,
      setGreetingMsg2,
    ]
  )

  // SSE 스트림 훅 호출

  useChatStream<EventDataMap>(chatroomId ?? '', userId, accessToken, handleSseEvent, e => {
    console.error('SSE Error (Network/Server)', e)
    setSseError('SSE 연결 중 오류가 발생했습니다.')
    stopInactivityTimer() // 타이머 완전 중지
  })

  return {
    isAiResponding,
    sseError,
    handleSendMessage,
  }
}
