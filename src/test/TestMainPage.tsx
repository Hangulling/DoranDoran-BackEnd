import { useNavigate, useParams } from 'react-router-dom'
import { useGoBack } from '../hooks/useGoBack'
import { useFetchUser } from '../hooks/useFetchUser'
import Banner from '../components/main/Banner'
import ChatRoomList from '../components/main/ChatRoomList'
import { useFetchTestChatRooms } from './useFetchTestChatRooms'

const TestMainPage = () => {
  const navigate = useNavigate()
  const { model } = useParams<{ model: 'a' | 'b' | 'c' }>()
  const { userName, userId } = useFetchUser()
  const { chatMsg, isLoading } = useFetchTestChatRooms(userId, model ?? 'a')

  // 뒤로 가기 방지
  useGoBack()

  const handleRoomClick = (id: number, roomName: string) => {
    navigate(`/test/closeness/${model}/${id}`, {
      state: { roomRouteId: id, concept: roomName, testModel: model },
    })
  }

  return (
    <div>
      <Banner userName={userName} />

      <div className="max-w-md mx-auto px-5 pb-16 mt-[30px]">
        <div className="text-title mb-4 text-[20px] border-b border-gray-80 pb-2">
          Chats (Test {model?.toUpperCase()})
        </div>

        <ChatRoomList isLoading={isLoading} chatMsg={chatMsg} onRoomClick={handleRoomClick} />

        <p className="mt-45 text-center text-gray-300 text-[12px]">
          Copyright 2025. dorandoran all rights reserved.
        </p>
      </div>
    </div>
  )
}

export default TestMainPage
