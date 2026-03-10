import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { createChatRoom } from '../api'
import useRoomIdStore from '../stores/useRoomIdStore'
import useClosenessStore from '../stores/useClosenessStore'
import ReactGA from 'react-ga4'
import { GA_ENABLED, IS_PROD } from '../constants/env'

interface CreateChatRoomParams {
  userId: string
  concept: string
  chatbotId: string
  intimacyLevel: number
}

export const useCreateChatRoom = (routeId: string) => {
  const navigate = useNavigate()
  const { addRoomMapping, setChatbotId } = useRoomIdStore.getState()
  const { setCloseness } = useClosenessStore.getState()

  return useMutation({
    mutationFn: (params: CreateChatRoomParams) => createChatRoom(params),

    onSuccess: (newRoom, variables) => {
      // set_intimacy_level
      if (IS_PROD && GA_ENABLED) {
        ReactGA.event('set_intimacy_level', {
          chatroom_id: newRoom.id,
          concept: variables.concept,
          intimacy_level: variables.intimacyLevel,
        })
      }

      addRoomMapping(routeId, newRoom.id)
      setChatbotId(routeId, variables.chatbotId)
      setCloseness(routeId, variables.intimacyLevel)
    },

    onError: error => {
      console.error('채팅방 생성 실패:', error)
      navigate('/error', { state: { from: `/closeness/${routeId}` } })
    },
  })
}
