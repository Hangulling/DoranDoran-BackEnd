import { useEffect, useMemo, useRef, useState } from 'react'
import CoachMark from '../components/chat/CoachMark'
import ChatFooter from '../components/chat/ChatFooter'
import type { Message } from '../types/chat'
import { useModalStore } from '../stores/useUiStateStore'
import { useNavigate, useParams } from 'react-router-dom'
import { chatRooms } from '../mocks/db/chat'
import ExitModal from '../components/chat/ExitModal'
import { useUserStore } from '../stores/useUserStore'
import useRoomIdStore from '../stores/useRoomIdStore'
import type { IntimacyAnalysisData, VocabularyExtractedData } from '../types/sseEvents'
import { getUserById } from '../api'
import useClosenessStore from '../stores/useClosenessStore'
import { getClosenessAsText } from '../utils/conceptMap'
import ReactGA from 'react-ga4'
import { GA_ENABLED, IS_PROD } from '../constants/env'
import { useBookmarkManager } from '../hooks/chat/useBookmarkManager'
import { useCoachMark } from '../hooks/chat/useCoachMark'
import { useChatExit } from '../hooks/chat/useChatExit'
import { useInactivityTimer } from '../hooks/chat/useInactivityTimer'
import { useChatHistory } from '../hooks/chat/useChatHistory'
import ChatBody from '../components/chat/ChatBody'
import { useChatInteraction } from '../hooks/chat/useChatInteraction'

const INACTIVITY_DURATION_MS = 300000

export interface EnrichedMessage extends Message {
  correction?: IntimacyAnalysisData | null // 교정 데이터 저장
  vocabularyData?: VocabularyExtractedData | null // 어휘 데이터 저장
  isPerfect?: boolean // Perfect 여부 저장
  analysisState?: 'pending' | 'complete' // 교정 데이터 로딩
  bookmarkId?: string | null
}

const chatBotIdByRoom = (conceptValue: string): string => {
  switch (conceptValue) {
    case '1':
      return '22222222-2222-2222-2222-222222222221'
    case '2':
      return '22222222-2222-2222-2222-222222222222'
    case '3':
      return '22222222-2222-2222-2222-222222222223'
    case '4':
      return '22222222-2222-2222-2222-222222222224'
    default:
      return ''
  }
}

const ChatPage: React.FC = () => {
  const navigate = useNavigate()
  const { id } = useParams()
  const chatbotId = chatBotIdByRoom(id ?? '')
  const setNoShowAgain = useModalStore(state => state.setNoShowAgain)
  const [messages, setMessages] = useState<EnrichedMessage[]>([]) // 확장
  const [isHistoryLoading, setIsHistoryLoading] = useState(true)
  const [isInitChatReady, setIsInitChatReady] = useState(false)
  const [isNewChat, setIsNewChat] = useState<boolean | null>(null)
  const [greetingState, setGreetingState] = useState<'pending' | 'loading' | 'complete'>('pending')
  const [greetingMsg1, setGreetingMsg1] = useState<string | null>(null)
  const [greetingMsg2, setGreetingMsg2] = useState<string | null>(null)
  const chatMainRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement | null>(null)
  const userId = useUserStore(state => state.id)
  const roomsMap = useRoomIdStore(state => state.roomsMap)
  const chatroomId = id ? roomsMap[id] : undefined
  const closenessLevel = useClosenessStore.getState().getCloseness(id ?? '') ?? 1
  const closenessText = getClosenessAsText(closenessLevel)
  const accessToken = sessionStorage.getItem('accessToken') ?? ''

  const room = useMemo(() => {
    return chatRooms.find(r => String(r.roomRouteId) === String(id))
  }, [id])

  const { showCoachMark, handleCloseCoachMark } = useCoachMark(userId, isInitChatReady)

  const { handleChatBubbleBookmark, handleCorrectionBubbleBookmark } = useBookmarkManager({
    chatroomId,
    chatbotId,
    closenessText,
    messages,
    setMessages,
  })

  const { isModalOpen, handleConfirmExit, handleCancelExit } = useChatExit({
    chatroomId,
    userId,
    routeId: id,
  })

  const { inactivityError, resetInactivityTimer, stopInactivityTimer } =
    useInactivityTimer(INACTIVITY_DURATION_MS)

  useChatHistory({
    chatroomId,
    userId,
    roomAvatar: room?.avatar,
    id,
    navigate,
    setMessages,
    setIsHistoryLoading,
    setIsNewChat,
    setGreetingState,
    setGreetingMsg1,
    setGreetingMsg2,
  })

  const { isAiResponding, sseError, handleSendMessage } = useChatInteraction({
    chatroomId,
    userId,
    accessToken,
    roomAvatar: room?.avatar,
    isNewChat,
    setMessages,
    resetInactivityTimer,
    stopInactivityTimer,
    setGreetingMsg1,
    setGreetingMsg2,
    setGreetingState,
  })

  useEffect(() => {
    if (IS_PROD && GA_ENABLED && chatroomId && userId) {
      const sessionKey = `viewed_chatroom_${chatroomId}`
      const alreadyViewed = sessionStorage.getItem(sessionKey)

      // alreadyViewed가 'true'가 아닐 때만 이벤트 전송
      if (alreadyViewed !== 'true') {
        const yyyyMmDd = new Date().toISOString().slice(0, 10)
        ReactGA.event('view_chatroom', {
          chatroom_id: chatroomId,
          date: yyyyMmDd,
        })
        // 이벤트 전송 후 sessionStorage에 플래그 설정
        sessionStorage.setItem(sessionKey, 'true')
      }
    }
  }, [chatroomId, userId]) // chatroomId와 userId가 확정되면 1회 실행

  // 봇/가이드 메시지 도착
  useEffect(() => {
    const checkCompletion = (botMsg: string, guideMsg: string | null) => {
      // 'loading' 상태는 isNewChat == true일 때만 설정됨
      if (greetingState !== 'loading') return // 중복 실행 방지

      if (IS_PROD && GA_ENABLED && chatroomId) {
        ReactGA.event('send_greeting_message', {
          chatroom_id: chatroomId,
          bot_message: botMsg,
          guide_message: guideMsg ?? '', // 가이드 메시지는 없을 수 있음
        })
      }
      setGreetingState('complete')
    }

    // 봇 메시지와 가이드 메시지가 모두 도착하면 'complete'
    if (greetingMsg1 && greetingMsg2) {
      checkCompletion(greetingMsg1, greetingMsg2)
      setGreetingState('complete')
      return
    }
    // InitChat 렌더링 시작
    if (greetingMsg1 && greetingState === 'loading') {
      setGreetingState('complete')
      return
    }
    // (새로고침 시) 봇 메시지만 있고 가이드가 없는 경우 즉시 완료
    if (
      greetingMsg1 &&
      !greetingMsg2 &&
      greetingState !== 'pending' &&
      greetingState !== 'loading'
    ) {
      setGreetingState('complete')
    }
  }, [userId, greetingMsg1, greetingMsg2, greetingState, chatroomId])

  // 다시 보지 않기 설정을 스토어에 동기화
  useEffect(() => {
    const fetchUserExitSetting = async () => {
      if (!userId) return

      try {
        const user = await getUserById(userId)
        if (user && user.exitModalDoNotShowAgain === true) {
          setNoShowAgain(true)
        }
      } catch (error) {
        console.error('Failed to fetch user exit modal setting:', error)
      }
    }

    fetchUserExitSetting()
  }, [userId, setNoShowAgain])

  useEffect(() => {
    if (chatMainRef.current) {
      chatMainRef.current.scrollTop = chatMainRef.current.scrollHeight
    }
  }, [messages])

  return (
    <div className="flex flex-col h-full overflow-hidden">
      <div ref={chatMainRef} className="flex-grow overflow-y-auto px-5 pt-10">
        <ChatBody
          isHistoryLoading={isHistoryLoading}
          greetingState={greetingState}
          roomAvatar={room?.avatar}
          onInitReady={() => setIsInitChatReady(true)}
          greetingMsg1={greetingMsg1}
          greetingMsg2={greetingMsg2}
          isNewChat={isNewChat}
          messages={messages}
          isAiResponding={isAiResponding}
          sseError={sseError}
          inactivityError={inactivityError}
          chatroomId={chatroomId}
          onChatBubbleBookmark={handleChatBubbleBookmark}
          onCorrectionBubbleBookmark={handleCorrectionBubbleBookmark}
        />
      </div>
      <CoachMark show={showCoachMark} onClose={handleCloseCoachMark} />
      <footer className="shrink-0">
        <ChatFooter
          inputRef={inputRef}
          onSendMessage={handleSendMessage}
          disabled={isHistoryLoading || !isInitChatReady || isAiResponding}
        />
      </footer>
      <ExitModal open={isModalOpen} onConfirm={handleConfirmExit} onCancel={handleCancelExit} />
    </div>
  )
}

export default ChatPage
