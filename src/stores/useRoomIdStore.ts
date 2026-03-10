import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

interface RoomIdState {
  roomsMap: Record<string, string>
  addRoomMapping: (numId: string, uuid: string) => void
  removeRoomMapping: (numId: string) => void
}

interface RoomIdState {
  roomsMap: Record<string, string>
  chatbotMap: Record<string, string>
  addRoomMapping: (numId: string, uuid: string) => void
  removeRoomMapping: (numId: string) => void
  setChatbotId: (numId: string, chatbotId: string) => void
  removeChatbotId: (numId: string) => void
}

const useRoomIdStore = create<RoomIdState & { reset: () => void }>()(
  persist(
    set => ({
      roomsMap: {},
      chatbotMap: {},

      addRoomMapping: (numId, uuid) =>
        set(state => ({
          roomsMap: { ...state.roomsMap, [numId]: uuid },
        })),

      removeRoomMapping: numId =>
        set(state => {
          const copy = { ...state.roomsMap }
          delete copy[numId]
          return { roomsMap: copy }
        }),

      setChatbotId: (numId, chatbotId) =>
        set(state => ({
          chatbotMap: { ...state.chatbotMap, [numId]: chatbotId },
        })),

      removeChatbotId: numId =>
        set(state => {
          const copy = { ...state.chatbotMap }
          delete copy[numId]
          return { chatbotMap: copy }
        }),

      reset: () => {
        set({ roomsMap: {}, chatbotMap: {} })
        sessionStorage.removeItem('room-id-storage')
      },
    }),
    {
      name: 'room-id-storage',
      storage: createJSONStorage(() => sessionStorage),
    }
  )
)

export default useRoomIdStore
