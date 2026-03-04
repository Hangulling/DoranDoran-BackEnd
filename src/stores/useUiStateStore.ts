import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

interface CoachMarkState {
  coachMarkSeen: boolean
  setCoachMarkSeen: (val: boolean) => void
}

interface ModalState {
  noShowAgain: boolean
  setNoShowAgain: (val: boolean) => void
}

export const useCoachStore = create<CoachMarkState & { reset: () => void }>()(
  persist(
    set => ({
      coachMarkSeen: false,
      setCoachMarkSeen: val => set({ coachMarkSeen: val }),
      reset: () => {
        set({ coachMarkSeen: false })
        sessionStorage.removeItem('coachMark-state')
      },
    }),
    { name: 'coachMark-state', storage: createJSONStorage(() => sessionStorage) }
  )
)

export const useModalStore = create<ModalState & { reset: () => void }>()(
  persist(
    set => ({
      noShowAgain: false,
      setNoShowAgain: val => set({ noShowAgain: val }),
      reset: () => {
        set({ noShowAgain: false })
        sessionStorage.removeItem('modal-state')
      },
    }),
    { name: 'modal-state', storage: createJSONStorage(() => sessionStorage) }
  )
)
