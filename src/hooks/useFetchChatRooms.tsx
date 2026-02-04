import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getLastInteractions } from '../api'
import { chatRooms } from '../mocks/db/chat'
import { getDaysDiff } from '../utils/getDaysDiff'
import { getChatbotValueById } from '../utils/chatbotMap'
import type { ChatRoomWithMessage } from '../types/main'

const getStatusMessage = (diffDays: number | undefined): string => {
  if (diffDays === undefined) return 'Start your first chat now'
  if (diffDays <= 0) return 'Talk again with a new topic' // 24시간 이내
  return 'It’s been a while! Let’s chat again'
}

export const useFetchChatRooms = (userId: string) => {
  const navigate = useNavigate()
  const [chatMsg, setChatMsg] = useState<ChatRoomWithMessage[]>([])
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    if (!userId) return

    setIsLoading(true)

    getLastInteractions(userId)
      .then(response => {
        const mergedRooms = chatRooms.map(mockRoom => {
          const serverRoom = response.find(r => {
            const serverValue = getChatbotValueById(r.chatbotId)
            const mockValue = String(mockRoom.roomRouteId)
            return serverValue === mockValue
          })

          const diffDays = serverRoom?.lastInteractionAt
            ? getDaysDiff(serverRoom.lastInteractionAt)
            : undefined

          const message = getStatusMessage(diffDays)

          return {
            ...mockRoom,
            message,
            concept: mockRoom.roomName,
          }
        })
        setChatMsg(mergedRooms)
      })
      .catch(err => {
        console.error('채팅방 목록 로드 실패:', err)
        navigate('/error', { state: { from: '/main' } })
      })
      .finally(() => {
        setIsLoading(false)
      })
  }, [userId, navigate])

  return { chatMsg, isLoading }
}
