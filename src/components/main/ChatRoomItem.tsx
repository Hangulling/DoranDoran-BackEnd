import type { ChatRoomItemProps } from '../../types/main'
import { capitalizeFirstLetter } from '../../utils/capitalizeFirstLetter'

const ChatRoomItem = ({ room, onClick }: ChatRoomItemProps) => {
  return (
    <button
      key={room.roomRouteId}
      onClick={() => onClick(room.roomRouteId, room.roomName)}
      className="flex items-center gap-4 w-full h-21 bg-white rounded-lg shadow-[1px_1px_10px_rgba(0,0,0,0.1)] py-3 px-4 hover:bg-green-80 active:bg-green-80"
    >
      <div className="w-13 h-13 rounded-full flex items-center justify-center overflow-hidden bg-gray-100">
        <img src={room.avatar} alt={room.roomName} className="w-full h-full object-cover" />
      </div>
      <div className="flex flex-col items-start">
        <span className="text-title text-[16px]">{capitalizeFirstLetter(room.roomName)}</span>
        <span className="text-gray-600 text-[14px]">{room.message}</span>
      </div>
    </button>
  )
}

export default ChatRoomItem
