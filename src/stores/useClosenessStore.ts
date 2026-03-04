import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

interface ClosenessState {
  closenessMap: Record<string, number>
  setCloseness: (roomId: string, value: number) => void
  getCloseness: (roomId: string) => number | undefined
}

const useClosenessStore = create(
  persist<ClosenessState & { reset: () => void }>(
    (set, get) => ({
      closenessMap: {},
      setCloseness: (roomId, value) => {
        set(state => ({
          closenessMap: { ...state.closenessMap, [roomId]: value },
        }))
      },
      getCloseness: roomId => {
        return get().closenessMap[roomId]
      },
      reset: () => {
        set({ closenessMap: {} })
        sessionStorage.removeItem('closeness-storage')
      },
    }),
    {
      name: 'closeness-storage',
      storage: createJSONStorage(() => sessionStorage),
    }
  )
)

export default useClosenessStore
