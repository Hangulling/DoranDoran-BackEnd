import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import useRoomIdStore from '../stores/useRoomIdStore'
import useClosenessStore from '../stores/useClosenessStore'
import { createTestChatRoom } from '../api'

// createTestChatRoom의 인자 타입
interface CreateTestChatRoomParams {
  userId: string
  concept: string
  chatbotId: string
  intimacyLevel: number
  testModel: 'a' | 'b' | 'c'
}

export const useCreateTestChatRoom = (routeId: string, testModel: 'a' | 'b' | 'c') => {
  const navigate = useNavigate()
  const { addRoomMapping, setChatbotId } = useRoomIdStore.getState()
  const { setCloseness } = useClosenessStore.getState()

  // useMutation 훅 사용
  return useMutation({
    // 1. 뮤테이션 함수 (API 호출)
    mutationFn: (params: CreateTestChatRoomParams) => createTestChatRoom(params),

    // 2. API 호출 성공 시
    onSuccess: (newRoom, variables) => {
      // (variables는 mutationFn에 전달된 params입니다)

      // Zustand 스토어 업데이트
      addRoomMapping(routeId, newRoom.id)
      setChatbotId(routeId, variables.chatbotId)
      setCloseness(routeId, variables.intimacyLevel)

      // 성공 시 페이지 이동 (애니메이션은 컴포넌트에서 처리)
    },

    // 3. API 호출 실패 시
    onError: error => {
      console.error('채팅방 생성 실패:', error)
      navigate('/error', { state: { from: `/test/closeness/${testModel}/${routeId}` } })
    },
  })
}
