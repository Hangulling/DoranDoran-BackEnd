export interface ChatRoomWithMessage {
  roomRouteId: number
  roomName: string
  concept: string
  avatar: string
  message: string
}

export interface ChatRoomItemProps {
  room: ChatRoomWithMessage
  onClick: (id: number, roomName: string) => void
}
