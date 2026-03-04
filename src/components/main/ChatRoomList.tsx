import type { ChatRoomWithMessage } from '../../types/main'
import LoadingSpinner from '../common/LoadingSpinner'
import ChatRoomItem from './ChatRoomItem'

interface ChatRoomListProps {
  isLoading: boolean
  chatMsg: ChatRoomWithMessage[]
  onRoomClick: (id: number, roomName: string) => void
}

const ChatRoomList = ({ isLoading, chatMsg, onRoomClick }: ChatRoomListProps) => {
  return (
    <div className="flex flex-col gap-[10px]">
      {isLoading ? (
        <LoadingSpinner />
      ) : (
        chatMsg.map(room => (
          <ChatRoomItem key={room.roomRouteId} room={room} onClick={onRoomClick} />
        ))
      )}
    </div>
  )
}

export default ChatRoomList
