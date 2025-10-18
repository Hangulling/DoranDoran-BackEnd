import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface CoachMarkState {
  coachMarkSeen: boolean
  setCoachMarkSeen: (val: boolean) => void
}

interface ModalState {
  noShowAgain: boolean
  setNoShowAgain: (val: boolean) => void
}

export const useCoachStore = create<CoachMarkState>()(
  persist(
    set => ({
      coachMarkSeen: false,
      setCoachMarkSeen: val => set({ coachMarkSeen: val }),
    }),
    { name: 'coachMark-state' }
  )
)

export const useModalStore = create<ModalState>()(
  persist(
    set => ({
      noShowAgain: false,
      setNoShowAgain: val => set({ noShowAgain: val }),
    }),
    { name: 'modal-state' }
  )
)
